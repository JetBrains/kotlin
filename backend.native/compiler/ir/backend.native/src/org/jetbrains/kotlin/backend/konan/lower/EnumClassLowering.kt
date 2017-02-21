package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.jvm.descriptors.createValueParameter
import org.jetbrains.kotlin.backend.jvm.descriptors.initialize
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanPlatform
import org.jetbrains.kotlin.backend.konan.descriptors.getKonanInternalFunctions
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.transform
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.classValueType
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.singletonList
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList

// TODO: cross-module usage.

internal data class LoweredSyntheticFunction(val functionDescriptor: FunctionDescriptor, val containingClass: ClassDescriptor)

internal data class LoweredEnumEntry(val implObject: ClassDescriptor, val valuesProperty: PropertyDescriptor,
                                     val itemGetter: FunctionDescriptor, val entriesMap: Map<Name, Int>)

internal class EnumUsageLowering(val context: Context,
                                 val loweredFunctions: Map<FunctionDescriptor, LoweredSyntheticFunction>,
                                 val loweredEnumEntries: Map<ClassDescriptor, LoweredEnumEntry>)
    : IrElementTransformerVoid(), FileLoweringPass {

    override fun lower(irFile: IrFile) {
        visitFile(irFile)
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue): IrExpression {
        val enumClassDescriptor = expression.descriptor.containingDeclaration as ClassDescriptor
        enumClassDescriptor.companionObjectDescriptor
        return loadEnumEntry(expression.startOffset, expression.endOffset, enumClassDescriptor, expression.descriptor.name)
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
        if(expression.descriptor.kind != ClassKind.ENUM_ENTRY)
            return super.visitGetObjectValue(expression)
        val enumClassDescriptor = expression.descriptor.containingDeclaration as ClassDescriptor
        return loadEnumEntry(expression.startOffset, expression.endOffset, enumClassDescriptor, expression.descriptor.name)
    }

    private fun loadEnumEntry(startOffset: Int, endOffset: Int, enumClassDescriptor: ClassDescriptor, name: Name): IrExpression {
        val loweredEnumEntry = loweredEnumEntries[enumClassDescriptor]!!
        val implObject = loweredEnumEntry.implObject
        val implObjectGetter = IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, implObject.classValueType!!, implObject)
        val valuesGetter = IrGetFieldImpl(startOffset, endOffset, loweredEnumEntry.valuesProperty).apply {
            receiver = implObjectGetter
        }
        val ordinal = loweredEnumEntry.entriesMap[name]!!
        return IrCallImpl(startOffset, endOffset, loweredEnumEntry.itemGetter).apply {
            dispatchReceiver = valuesGetter
            putValueArgument(0, IrConstImpl.int(startOffset, endOffset, enumClassDescriptor.module.builtIns.intType, ordinal))
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val loweredFunction = loweredFunctions[expression.descriptor] ?: return super.visitCall(expression)
        return IrCallImpl(expression.startOffset, expression.endOffset, loweredFunction.functionDescriptor).apply {
            val containingClass = loweredFunction.containingClass
            dispatchReceiver = IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, containingClass.classValueType!!, containingClass)
            expression.descriptor.valueParameters.forEach { p -> putValueArgument(p, expression.getValueArgument(p)) }
        }
    }
}

internal class EnumClassLowering(val context: Context) : ClassLoweringPass {
    val loweredFunctions = mutableMapOf<FunctionDescriptor, LoweredSyntheticFunction>()
    val loweredEnumEntries = mutableMapOf<ClassDescriptor, LoweredEnumEntry>()

    fun run(irFile: IrFile) {
        runOnFilePostfix(irFile)
        EnumUsageLowering(context, loweredFunctions, loweredEnumEntries).lower(irFile)
    }

    override fun lower(irClass: IrClass) {
        val descriptor = irClass.descriptor
        if (descriptor.kind != ClassKind.ENUM_CLASS)
            return
        EnumClassTransformer(irClass).run()
    }

    private interface EnumConstructorCallTransformer {
        fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression
        fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression
    }

    private inner class EnumClassTransformer(val irClass: IrClass) {
        private val enumEntryOrdinals = mutableMapOf<ClassDescriptor, Int>()
        private val loweredEnumConstructors = mutableMapOf<ClassConstructorDescriptor, ClassConstructorDescriptor>()
        private val defaultEnumEntryConstructors = mutableMapOf<ClassConstructorDescriptor, ClassConstructorDescriptor>()
        private val loweredEnumConstructorParameters = mutableMapOf<ValueParameterDescriptor, ValueParameterDescriptor>()

        private lateinit var valuesFieldDescriptor: PropertyDescriptor
        private lateinit var valuesFunctionDescriptor: FunctionDescriptor
        private lateinit var valueOfFunctionDescriptor: FunctionDescriptor

        fun run() {
            assignOrdinalsToEnumEntries()
            lowerEnumConstructors(irClass)
            lowerEnumEntriesClasses()
            val defaultClass = createDefaultClassForEnumEntries()
            lowerEnumClassBody()
            if (defaultClass != null)
                irClass.declarations.add(defaultClass)
            createImplObject()
        }

        private fun assignOrdinalsToEnumEntries() {
            var ordinal = 0
            irClass.declarations.forEach {
                if (it is IrEnumEntry) {
                    enumEntryOrdinals.put(it.descriptor, ordinal)
                    ordinal++
                }
            }
        }

        private fun lowerEnumEntriesClasses() {
            irClass.declarations.transformFlat { declaration ->
                if (declaration is IrEnumEntry) {
                    declaration.singletonList() + lowerEnumEntryClass(declaration.correspondingClass).singletonOrEmptyList()
                } else null
            }
        }

        private fun lowerEnumEntryClass(enumEntryClass: IrClass?): IrClass? {
            if (enumEntryClass == null) return null

            lowerEnumConstructors(enumEntryClass)

            return enumEntryClass
        }

        private fun createDefaultClassForEnumEntries(): IrClass? {
            if (!irClass.declarations.any({ it is IrEnumEntry && it.correspondingClass == null })) return null
            val descriptor = irClass.descriptor
            val defaultClassDescriptor = ClassDescriptorImpl(descriptor, "DEFAULT".synthesizedName, Modality.FINAL,
                    ClassKind.CLASS, descriptor.defaultType.singletonList(), SourceElement.NO_SOURCE, false)
            val defaultClass = IrClassImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.DEFINED, defaultClassDescriptor)

            val constructors = mutableSetOf<ClassConstructorDescriptor>()

            descriptor.constructors.forEach {
                val loweredEnumConstructor = loweredEnumConstructors[it]!!
                val (constructorDescriptor, constructor) = createSimpleDelegatingConstructor(defaultClassDescriptor, loweredEnumConstructor)
                constructors.add(constructorDescriptor)
                defaultClass.declarations.add(constructor)
                defaultEnumEntryConstructors.put(loweredEnumConstructor, constructorDescriptor)
            }

            val memberScope = MemberScope.Empty
            defaultClassDescriptor.initialize(memberScope, constructors, null)

            return defaultClass
        }

        private fun createImplObject() {
            val descriptor = irClass.descriptor
            val implObjectDescriptor = ClassDescriptorImpl(descriptor, "OBJECT".synthesizedName, Modality.FINAL,
                    ClassKind.OBJECT, KonanPlatform.builtIns.anyType.singletonList(), SourceElement.NO_SOURCE, false)
            val implObject = IrClassImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.DEFINED, implObjectDescriptor)

            val enumEntries = mutableListOf<IrEnumEntry>()
            var i = 0
            while (i < irClass.declarations.size) {
                val declaration = irClass.declarations[i]
                var delete = false
                when (declaration) {
                    is IrEnumEntry -> {
                        enumEntries.add(declaration)
                        delete = true
                    }
                    is IrFunction -> {
                        var copiedDescriptor = tryCopySyntheticBodyDeclaration(implObjectDescriptor, declaration, IrSyntheticBodyKind.ENUM_VALUES)
                        if(copiedDescriptor != null)
                        {
                            valuesFunctionDescriptor = copiedDescriptor
                            delete = true
                        }
                        copiedDescriptor = tryCopySyntheticBodyDeclaration(implObjectDescriptor, declaration, IrSyntheticBodyKind.ENUM_VALUEOF)
                        if(copiedDescriptor != null)
                        {
                            valueOfFunctionDescriptor = copiedDescriptor
                            delete = true
                        }
                    }
                }
                if (delete)
                    irClass.declarations.removeAt(i)
                else
                    ++i
            }

            val memberScope = MemberScope.Empty

            val constructorOfAny = irClass.descriptor.module.builtIns.any.constructors.first()
            val (constructorDescriptor, constructor) = createSimpleDelegatingConstructor(implObjectDescriptor, constructorOfAny)

            implObjectDescriptor.initialize(memberScope, setOf(constructorDescriptor), constructorDescriptor)

            implObject.declarations.add(constructor)
            implObject.declarations.add(createSyntheticValuesFieldDeclaration(implObjectDescriptor, enumEntries))
            implObject.declarations.add(createSyntheticValuesMethodDeclaration(implObjectDescriptor))
            implObject.declarations.add(createSyntheticValueOfMethodDeclaration(implObjectDescriptor))

            irClass.declarations.add(implObject)
        }

        private fun tryCopySyntheticBodyDeclaration(implObjectDescriptor: ClassDescriptor,
                                                    declaration: IrFunction,
                                                    kind: IrSyntheticBodyKind): FunctionDescriptor? {
            if (!declaration.body.let { it is IrSyntheticBody && it.kind == kind })
                return null
            val newDescriptor = declaration.descriptor
                    .newCopyBuilder()
                    .setOwner(implObjectDescriptor)
                    .setName(declaration.descriptor.name.identifier.synthesizedName)
                    .setDispatchReceiverParameter(implObjectDescriptor.thisAsReceiverParameter)
                    .build()!!
            loweredFunctions.put(declaration.descriptor, LoweredSyntheticFunction(newDescriptor, implObjectDescriptor))
            return newDescriptor
        }

        private fun createSimpleDelegatingConstructor(classDescriptor: ClassDescriptor,
                                                      superConstructorDescriptor: ClassConstructorDescriptor)
                : Pair<ClassConstructorDescriptor, IrConstructor> {
            val constructorDescriptor = ClassConstructorDescriptorImpl.createSynthesized(
                    classDescriptor,
                    Annotations.EMPTY,
                    superConstructorDescriptor.isPrimary,
                    SourceElement.NO_SOURCE)
            val valueParameters = superConstructorDescriptor.valueParameters.map {
                it.copy(constructorDescriptor, it.name, it.index)
            }
            constructorDescriptor.initialize(valueParameters, superConstructorDescriptor.visibility)
            constructorDescriptor.returnType = superConstructorDescriptor.returnType

            val body = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    listOf(
                            IrDelegatingConstructorCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, superConstructorDescriptor).apply {
                                valueParameters.forEachIndexed { idx, parameter ->
                                    putValueArgument(idx, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, parameter))
                                }
                            },
                            IrInstanceInitializerCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, classDescriptor)
                    )
            )
            val constructor = IrConstructorImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.DEFINED, constructorDescriptor, body)

            return Pair(constructorDescriptor, constructor)
        }

        private fun createSyntheticValuesFieldDeclaration(implObjectDescriptor: ClassDescriptor,
                                                          enumEntries: List<IrEnumEntry>): IrFieldImpl {
            val valuesArrayType = context.builtIns.getArrayType(Variance.INVARIANT, irClass.descriptor.defaultType)
            valuesFieldDescriptor = createSyntheticValuesFieldDescriptor(implObjectDescriptor, valuesArrayType)

            val getter = genericArrayType.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("get"), NoLookupLocation.FROM_BACKEND).single()

            val typeParameterT = genericArrayType.declaredTypeParameters[0]
            val enumClassType = irClass.descriptor.defaultType
            val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameterT.typeConstructor to TypeProjectionImpl(enumClassType)))
            val substitutedValueOf = getter.substitute(typeSubstitutor)!!

            val entriesMap = enumEntries.associateBy({ it -> it.descriptor.name }, { it -> enumEntryOrdinals[it.descriptor]!! }).toMap()
            val loweredEnumEntry = LoweredEnumEntry(implObjectDescriptor, valuesFieldDescriptor, substitutedValueOf, entriesMap)
            loweredEnumEntries.put(irClass.descriptor, loweredEnumEntry)

            val irValuesInitializer = createArrayOfExpression(irClass.descriptor.defaultType, enumEntries.map { it.initializerExpression })

            val irField = IrFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.DEFINED,
                    valuesFieldDescriptor,
                    IrExpressionBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irValuesInitializer))
            return irField
        }

        private fun createSyntheticValuesFieldDescriptor(implObjectDescriptor: ClassDescriptor, valuesArrayType: SimpleType): PropertyDescriptorImpl {
            val receiver = ReceiverParameterDescriptorImpl(implObjectDescriptor, ImplicitClassReceiver(implObjectDescriptor))
            return PropertyDescriptorImpl.create(implObjectDescriptor, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE,
                    false, "VALUES".synthesizedName, CallableMemberDescriptor.Kind.SYNTHESIZED, irClass.descriptor.source,
                    false, false, false, false, false, false).initialize(valuesArrayType, dispatchReceiverParameter = receiver)
        }

        private val kotlinPackage = context.irModule!!.descriptor.getPackage(FqName("kotlin"))

        private val genericArrayOfFun = kotlinPackage.memberScope.getContributedFunctions(Name.identifier("arrayOf"), NoLookupLocation.FROM_BACKEND).first()

        private val genericValueOfFun = context.builtIns.getKonanInternalFunctions("valueOfForEnum").single()

        private val genericValuesFun = context.builtIns.getKonanInternalFunctions("valuesForEnum").single()

        private val genericArrayType = kotlinPackage.memberScope.getContributedClassifier(Name.identifier("Array"), NoLookupLocation.FROM_BACKEND) as ClassDescriptor

        private fun createArrayOfExpression(arrayElementType: KotlinType, arrayElements: List<IrExpression>): IrExpression {
            val typeParameter0 = genericArrayOfFun.typeParameters[0]
            val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameter0.typeConstructor to TypeProjectionImpl(arrayElementType)))
            val substitutedArrayOfFun = genericArrayOfFun.substitute(typeSubstitutor)!!

            val typeArguments = mapOf(typeParameter0 to arrayElementType)

            val valueParameter0 = substitutedArrayOfFun.valueParameters[0]
            val arg0VarargType = valueParameter0.type
            val arg0VarargElementType = valueParameter0.varargElementType!!
            val arg0 = IrVarargImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, arg0VarargType, arg0VarargElementType, arrayElements)

            return IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, substitutedArrayOfFun, typeArguments).apply {
                putValueArgument(0, arg0)
            }
        }

        private fun createSyntheticValuesMethodDeclaration(companionObjectDescriptor: ClassDescriptor): IrFunction {
            val typeParameterT = genericValuesFun.typeParameters[0]
            val enumClassType = irClass.descriptor.defaultType
            val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameterT.typeConstructor to TypeProjectionImpl(enumClassType)))
            val substitutedValueOf = genericValuesFun.substitute(typeSubstitutor)!!

            val irValuesCall = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, substitutedValueOf, mapOf(typeParameterT to enumClassType))
                    .apply {
                        val receiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, companionObjectDescriptor.thisAsReceiverParameter)
                        putValueArgument(0, IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, valuesFieldDescriptor, receiver))
                    }

            val body = IrBlockBodyImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    listOf(IrReturnImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, valuesFunctionDescriptor, irValuesCall))
            )
            return IrFunctionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.DEFINED, valuesFunctionDescriptor, body)
        }

        private fun createSyntheticValueOfMethodDeclaration(companionObjectDescriptor: ClassDescriptor): IrFunction {
            val typeParameterT = genericValueOfFun.typeParameters[0]
            val enumClassType = irClass.descriptor.defaultType
            val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameterT.typeConstructor to TypeProjectionImpl(enumClassType)))
            val substitutedValueOf = genericValueOfFun.substitute(typeSubstitutor)!!

            val irValueOfCall = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, substitutedValueOf, mapOf(typeParameterT to enumClassType))
                    .apply {
                        putValueArgument(0, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, valueOfFunctionDescriptor.valueParameters[0]))
                        val receiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, companionObjectDescriptor.thisAsReceiverParameter)
                        putValueArgument(1, IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, valuesFieldDescriptor, receiver))
                    }

            val body = IrBlockBodyImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    listOf(IrReturnImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, valueOfFunctionDescriptor, irValueOfCall))
            )
            return IrFunctionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.DEFINED, valueOfFunctionDescriptor, body)
        }

        private fun lowerEnumConstructors(irClass: IrClass) {
            irClass.declarations.transform { declaration ->
                if (declaration is IrConstructor)
                    transformEnumConstructor(declaration)
                else
                    declaration
            }
        }

        private fun transformEnumConstructor(enumConstructor: IrConstructor): IrConstructor {
            val constructorDescriptor = enumConstructor.descriptor
            val loweredConstructorDescriptor = lowerEnumConstructor(constructorDescriptor)
            val loweredEnumConstructor = IrConstructorImpl(
                    enumConstructor.startOffset, enumConstructor.endOffset, enumConstructor.origin,
                    loweredConstructorDescriptor,
                    enumConstructor.body!! // will be transformed later
            )
            return loweredEnumConstructor
        }

        private fun lowerEnumConstructor(constructorDescriptor: ClassConstructorDescriptor): ClassConstructorDescriptor {
            val loweredConstructorDescriptor = ClassConstructorDescriptorImpl.createSynthesized(
                    constructorDescriptor.containingDeclaration,
                    constructorDescriptor.annotations,
                    constructorDescriptor.isPrimary,
                    constructorDescriptor.source
            )

            val valueParameters =
                    listOf(
                            loweredConstructorDescriptor.createValueParameter(0, "name", context.builtIns.stringType),
                            loweredConstructorDescriptor.createValueParameter(1, "ordinal", context.builtIns.intType)
                    ) +
                            constructorDescriptor.valueParameters.map {
                                lowerConstructorValueParameter(loweredConstructorDescriptor, it)
                            }
            loweredConstructorDescriptor.initialize(valueParameters, Visibilities.PROTECTED)

            loweredConstructorDescriptor.returnType = constructorDescriptor.returnType

            loweredEnumConstructors[constructorDescriptor] = loweredConstructorDescriptor

            return loweredConstructorDescriptor
        }

        private fun lowerConstructorValueParameter(
                loweredConstructorDescriptor: ClassConstructorDescriptor,
                valueParameterDescriptor: ValueParameterDescriptor
        ): ValueParameterDescriptor {
            val loweredValueParameterDescriptor = valueParameterDescriptor.copy(
                    loweredConstructorDescriptor,
                    valueParameterDescriptor.name,
                    valueParameterDescriptor.index + 2
            )
            loweredEnumConstructorParameters[valueParameterDescriptor] = loweredValueParameterDescriptor
            return loweredValueParameterDescriptor
        }

        private fun lowerEnumClassBody() {
            irClass.transformChildrenVoid(EnumClassBodyTransformer())
        }

        private inner class InEnumClassConstructor(val enumClassConstructor: ClassConstructorDescriptor) :
                EnumConstructorCallTransformer {
            override fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression {
                val startOffset = enumConstructorCall.startOffset
                val endOffset = enumConstructorCall.endOffset
                val origin = enumConstructorCall.origin

                val result = IrDelegatingConstructorCallImpl(startOffset, endOffset, enumConstructorCall.descriptor)

                assert(result.descriptor.valueParameters.size == 2) {
                    "Enum(String, Int) constructor call expected:\n${result.dump()}"
                }

                val nameParameter = enumClassConstructor.valueParameters.getOrElse(0) {
                    throw AssertionError("No 'name' parameter in enum constructor: $enumClassConstructor")
                }

                val ordinalParameter = enumClassConstructor.valueParameters.getOrElse(1) {
                    throw AssertionError("No 'ordinal' parameter in enum constructor: $enumClassConstructor")
                }

                result.putValueArgument(0, IrGetValueImpl(startOffset, endOffset, nameParameter, origin))
                result.putValueArgument(1, IrGetValueImpl(startOffset, endOffset, ordinalParameter, origin))

                return result
            }

            override fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression {
                val descriptor = delegatingConstructorCall.descriptor
                val startOffset = delegatingConstructorCall.startOffset
                val endOffset = delegatingConstructorCall.endOffset

                val loweredDelegatedConstructor = loweredEnumConstructors.getOrElse(descriptor) {
                    throw AssertionError("Constructor called in enum entry initializer should've been lowered: $descriptor")
                }

                val result = IrDelegatingConstructorCallImpl(startOffset, endOffset, loweredDelegatedConstructor)

                result.putValueArgument(0, IrGetValueImpl(startOffset, endOffset, enumClassConstructor.valueParameters[0]))
                result.putValueArgument(1, IrGetValueImpl(startOffset, endOffset, enumClassConstructor.valueParameters[1]))

                descriptor.valueParameters.forEach { valueParameter ->
                    val i = valueParameter.index
                    result.putValueArgument(i + 2, delegatingConstructorCall.getValueArgument(i))
                }

                return result
            }
        }

        private abstract inner class InEnumEntry(private val enumEntry: ClassDescriptor) : EnumConstructorCallTransformer {
            override fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression {
                val name = enumEntry.name.asString()
                val ordinal = enumEntryOrdinals[enumEntry]!!

                val descriptor = enumConstructorCall.descriptor
                val startOffset = enumConstructorCall.startOffset
                val endOffset = enumConstructorCall.endOffset

                val loweredConstructor = loweredEnumConstructors.getOrElse(descriptor) {
                    throw AssertionError("Constructor called in enum entry initializer should've been lowered: $descriptor")
                }

                val result = createConstructorCall(startOffset, endOffset, loweredConstructor)

                result.putValueArgument(0, IrConstImpl.string(startOffset, endOffset, context.builtIns.stringType, name))
                result.putValueArgument(1, IrConstImpl.int(startOffset, endOffset, context.builtIns.intType, ordinal))

                descriptor.valueParameters.forEach { valueParameter ->
                    val i = valueParameter.index
                    result.putValueArgument(i + 2, enumConstructorCall.getValueArgument(i))
                }

                return result
            }

            override fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression {
                throw AssertionError("Unexpected delegating constructor call within enum entry: $enumEntry")
            }

            abstract fun createConstructorCall(startOffset: Int, endOffset: Int, loweredConstructor: ClassConstructorDescriptor): IrMemberAccessExpression
        }

        private inner class InEnumEntryClassConstructor(enumEntry: ClassDescriptor) : InEnumEntry(enumEntry) {
            override fun createConstructorCall(startOffset: Int, endOffset: Int, loweredConstructor: ClassConstructorDescriptor)
                    = IrDelegatingConstructorCallImpl(startOffset, endOffset, loweredConstructor)
        }

        private inner class InEnumEntryInitializer(enumEntry: ClassDescriptor) : InEnumEntry(enumEntry) {
            override fun createConstructorCall(startOffset: Int, endOffset: Int, loweredConstructor: ClassConstructorDescriptor)
                    = IrCallImpl(startOffset, endOffset, defaultEnumEntryConstructors[loweredConstructor] ?: loweredConstructor)
        }

        private inner class EnumClassBodyTransformer : IrElementTransformerVoid() {
            private var enumConstructorCallTransformer: EnumConstructorCallTransformer? = null

            override fun visitEnumEntry(declaration: IrEnumEntry): IrStatement {
                assert(enumConstructorCallTransformer == null) { "Nested enum entry initialization:\n${declaration.dump()}" }

                enumConstructorCallTransformer = InEnumEntryInitializer(declaration.descriptor)

                var result: IrEnumEntry = IrEnumEntryImpl(declaration.startOffset, declaration.endOffset, declaration.origin,
                        declaration.descriptor, null, declaration.initializerExpression)
                result = super.visitEnumEntry(result) as IrEnumEntry

                enumConstructorCallTransformer = null

                return result
            }

            override fun visitConstructor(declaration: IrConstructor): IrStatement {
                val constructorDescriptor = declaration.descriptor
                val containingClass = constructorDescriptor.containingDeclaration

                // TODO local (non-enum) class in enum class constructor?
                val previous = enumConstructorCallTransformer

                if (containingClass.kind == ClassKind.ENUM_ENTRY) {
                    assert(enumConstructorCallTransformer == null) { "Nested enum entry initialization:\n${declaration.dump()}" }
                    enumConstructorCallTransformer = InEnumEntryClassConstructor(containingClass)
                } else if (containingClass.kind == ClassKind.ENUM_CLASS) {
                    assert(enumConstructorCallTransformer == null) { "Nested enum entry initialization:\n${declaration.dump()}" }
                    enumConstructorCallTransformer = InEnumClassConstructor(constructorDescriptor)
                }

                val result = super.visitConstructor(declaration)

                enumConstructorCallTransformer = previous

                return result
            }

            override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                val callTransformer = enumConstructorCallTransformer ?:
                        throw AssertionError("Enum constructor call outside of enum entry initialization or enum class constructor:\n" + irClass.dump())


                return callTransformer.transform(expression)
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                if (expression.descriptor.containingDeclaration.kind == ClassKind.ENUM_CLASS) {
                    val callTransformer = enumConstructorCallTransformer ?:
                            throw AssertionError("Enum constructor call outside of enum entry initialization or enum class constructor:\n" + irClass.dump())

                    return callTransformer.transform(expression)
                }
                return expression
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val loweredParameter = loweredEnumConstructorParameters[expression.descriptor]
                if (loweredParameter != null)
                    return IrGetValueImpl(expression.startOffset, expression.endOffset, loweredParameter, expression.origin)
                else
                    return expression
            }
        }
    }
}