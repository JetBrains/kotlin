/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.SimpleMemberScope
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.jvm.descriptors.createValueParameter
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.getKonanInternalFunctions
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.createArrayOfExpression
import org.jetbrains.kotlin.backend.konan.ir.createFakeOverrideDescriptor
import org.jetbrains.kotlin.backend.konan.ir.createSimpleDelegatingConstructor
import org.jetbrains.kotlin.backend.konan.ir.createSimpleDelegatingConstructorDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.transform
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor

internal class EnumSyntheticFunctionsBuilder(val context: Context) {
    fun buildValuesExpression(startOffset: Int, endOffset: Int,
                              enumClassDescriptor: ClassDescriptor): IrExpression {
        val loweredEnum = context.specialDescriptorsFactory.getLoweredEnum(enumClassDescriptor)

        val typeParameterT = genericValuesDescriptor.typeParameters[0]
        val enumClassType = enumClassDescriptor.defaultType
        val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameterT.typeConstructor to TypeProjectionImpl(enumClassType)))
        val substitutedValueOf = genericValuesDescriptor.substitute(typeSubstitutor)!!

        return IrCallImpl(startOffset, endOffset, substitutedValueOf, mapOf(typeParameterT to enumClassType))
                .apply {
                    val receiver = IrGetObjectValueImpl(startOffset, endOffset,
                            loweredEnum.implObjectDescriptor.defaultType, loweredEnum.implObjectDescriptor)
                    putValueArgument(0, IrGetFieldImpl(startOffset, endOffset, loweredEnum.valuesProperty, receiver))
                }
    }

    fun buildValueOfExpression(startOffset: Int, endOffset: Int,
                               enumClassDescriptor: ClassDescriptor,
                               value: IrExpression): IrExpression {
        val loweredEnum = context.specialDescriptorsFactory.getLoweredEnum(enumClassDescriptor)

        val typeParameterT = genericValueOfDescriptor.typeParameters[0]
        val enumClassType = enumClassDescriptor.defaultType
        val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameterT.typeConstructor to TypeProjectionImpl(enumClassType)))
        val substitutedValueOf = genericValueOfDescriptor.substitute(typeSubstitutor)!!

        return IrCallImpl(startOffset, endOffset, substitutedValueOf, mapOf(typeParameterT to enumClassType))
                .apply {
                    putValueArgument(0, value)
                    val receiver = IrGetObjectValueImpl(startOffset, endOffset,
                            loweredEnum.implObjectDescriptor.defaultType, loweredEnum.implObjectDescriptor)
                    putValueArgument(1, IrGetFieldImpl(startOffset, endOffset, loweredEnum.valuesProperty, receiver))
                }
    }

    private val genericValueOfDescriptor = context.builtIns.getKonanInternalFunctions("valueOfForEnum").single()

    private val genericValuesDescriptor = context.builtIns.getKonanInternalFunctions("valuesForEnum").single()
}

internal class EnumUsageLowering(val context: Context)
    : IrElementTransformerVoid(), FileLoweringPass {

    private val enumSyntheticFunctionsBuilder = EnumSyntheticFunctionsBuilder(context)

    override fun lower(irFile: IrFile) {
        visitFile(irFile)
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue): IrExpression {
        val enumClassDescriptor = expression.descriptor.containingDeclaration as ClassDescriptor
        return loadEnumEntry(expression.startOffset, expression.endOffset, enumClassDescriptor, expression.descriptor.name)
    }

    // TODO: remove as soon IR is fixed (there should no be any enum get with GET_OBJECT operation).
    override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
        if (expression.descriptor.kind != ClassKind.ENUM_ENTRY)
            return super.visitGetObjectValue(expression)
        val enumClassDescriptor = expression.descriptor.containingDeclaration as ClassDescriptor
        return loadEnumEntry(expression.startOffset, expression.endOffset, enumClassDescriptor, expression.descriptor.name)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)
        val descriptor = expression.descriptor as? FunctionDescriptor
        if (descriptor == null) return expression
        if (descriptor.original != enumValuesDescriptor && descriptor.original != enumValueOfDescriptor) return expression

        val genericT = descriptor.original.typeParameters[0]
        val substitutedT = expression.getTypeArgument(genericT)!!
        val classDescriptor = substitutedT.constructor.declarationDescriptor as? ClassDescriptor
        if (classDescriptor == null) return expression // Type parameter.

        assert (classDescriptor.kind == ClassKind.ENUM_CLASS)

        if (descriptor.original == enumValuesDescriptor) {
            return enumSyntheticFunctionsBuilder.buildValuesExpression(expression.startOffset, expression.endOffset, classDescriptor)
        } else {
            val value = expression.getValueArgument(0)!!
            return enumSyntheticFunctionsBuilder.buildValueOfExpression(expression.startOffset, expression.endOffset, classDescriptor, value)
        }
    }

    private val kotlinPackageMemberScope = context.builtIns.builtInsModule.getPackage(FqName("kotlin")).memberScope
    private val enumValueOfDescriptor = kotlinPackageMemberScope.getContributedFunctions(
            Name.identifier("enumValueOf"), NoLookupLocation.FROM_BACKEND).single()
    private val enumValuesDescriptor = kotlinPackageMemberScope.getContributedFunctions(
            Name.identifier("enumValues"), NoLookupLocation.FROM_BACKEND).single()

    private fun loadEnumEntry(startOffset: Int, endOffset: Int, enumClassDescriptor: ClassDescriptor, name: Name): IrExpression {
        val loweredEnum = context.specialDescriptorsFactory.getLoweredEnum(enumClassDescriptor)
        val ordinal = loweredEnum.entriesMap[name]!!
        return IrCallImpl(startOffset, endOffset, loweredEnum.itemGetter).apply {
            dispatchReceiver = IrCallImpl(startOffset, endOffset, loweredEnum.valuesGetter)
            putValueArgument(0, IrConstImpl.int(startOffset, endOffset, enumClassDescriptor.module.builtIns.intType, ordinal))
        }
    }
}

internal class EnumClassLowering(val context: Context) : ClassLoweringPass {
    fun run(irFile: IrFile) {
        runOnFilePostfix(irFile)
        EnumUsageLowering(context).lower(irFile)
    }

    override fun lower(irClass: IrClass) {
        val descriptor = irClass.descriptor
        if (descriptor.kind != ClassKind.ENUM_CLASS) return
        EnumClassTransformer(irClass).run()
    }

    private interface EnumConstructorCallTransformer {
        fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression
        fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression
    }

    private inner class EnumClassTransformer(val irClass: IrClass) {
        private val loweredEnum = context.specialDescriptorsFactory.getLoweredEnum(irClass.descriptor)
        private val enumEntryOrdinals = mutableMapOf<ClassDescriptor, Int>()
        private val loweredEnumConstructors = mutableMapOf<ClassConstructorDescriptor, ClassConstructorDescriptor>()
        private val descriptorToIrConstructorWithDefaultArguments = mutableMapOf<ClassConstructorDescriptor, IrConstructor>()
        private val defaultEnumEntryConstructors = mutableMapOf<ClassConstructorDescriptor, ClassConstructorDescriptor>()
        private val loweredEnumConstructorParameters = mutableMapOf<ValueParameterDescriptor, ValueParameterDescriptor>()
        private val enumSyntheticFunctionsBuilder = EnumSyntheticFunctionsBuilder(context)

        fun run() {
            insertInstanceInitializerCall()
            assignOrdinalsToEnumEntries()
            lowerEnumConstructors(irClass)
            lowerEnumEntriesClasses()
            val defaultClass = createDefaultClassForEnumEntries()
            lowerEnumClassBody()
            if (defaultClass != null)
                irClass.declarations.add(defaultClass)
            createImplObject()
        }

        private fun insertInstanceInitializerCall() {
            irClass.transformChildrenVoid(object: IrElementTransformerVoid() {
                override fun visitClass(declaration: IrClass): IrStatement {
                    // Skip nested
                    return declaration
                }

                override fun visitConstructor(declaration: IrConstructor): IrStatement {
                    declaration.transformChildrenVoid(this)

                    val blockBody = declaration.body as? IrBlockBody
                            ?: throw AssertionError("Unexpected constructor body: ${declaration.body}")
                    if (blockBody.statements.all { it !is IrInstanceInitializerCall }) {
                        blockBody.statements.transformFlat {
                            if (it is IrEnumConstructorCall)
                                listOf(it, IrInstanceInitializerCallImpl(declaration.startOffset, declaration.startOffset,
                                        irClass.descriptor))
                            else null
                        }
                    }
                    return declaration
                }
            })
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
                    listOfNotNull(declaration, lowerEnumEntryClass(declaration.correspondingClass))
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
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset
            val descriptor = irClass.descriptor
            val defaultClassDescriptor = ClassDescriptorImpl(descriptor, "DEFAULT".synthesizedName, Modality.FINAL,
                    ClassKind.CLASS, listOf(descriptor.defaultType), SourceElement.NO_SOURCE, false)
            val defaultClass = IrClassImpl(startOffset, endOffset, IrDeclarationOrigin.DEFINED, defaultClassDescriptor)

            defaultClass.createParameterDeclarations()

            val constructors = mutableSetOf<ClassConstructorDescriptor>()

            descriptor.constructors.forEach {
                val loweredEnumConstructor = loweredEnumConstructors[it]!!
                val constructorDescriptor = defaultClassDescriptor.createSimpleDelegatingConstructorDescriptor(loweredEnumConstructor)
                val constructor = defaultClassDescriptor.createSimpleDelegatingConstructor(
                        loweredEnumConstructor, constructorDescriptor,
                        startOffset, endOffset, DECLARATION_ORIGIN_ENUM)
                constructors.add(constructorDescriptor)
                defaultClass.declarations.add(constructor)
                defaultEnumEntryConstructors.put(loweredEnumConstructor, constructorDescriptor)

                val irConstructor = descriptorToIrConstructorWithDefaultArguments[loweredEnumConstructor]
                if (irConstructor != null) {
                    it.valueParameters.filter { it.declaresDefaultValue() }.forEach { argument ->
                        val loweredArgument = loweredEnumConstructor.valueParameters[argument.loweredIndex()]
                        val body = irConstructor.getDefault(loweredArgument)!!
                        body.transformChildrenVoid(ParameterMapper(constructorDescriptor))
                        constructor.putDefault(constructorDescriptor.valueParameters[loweredArgument.index], body)
                    }
                }
            }

            val contributedDescriptors = irClass.descriptor.unsubstitutedMemberScope
                    .getContributedDescriptors()
                    .map { it.createFakeOverrideDescriptor(defaultClassDescriptor) }
                    .filterNotNull()
                    .toList()
            defaultClassDescriptor.initialize(SimpleMemberScope(contributedDescriptors), constructors, null)

            return defaultClass
        }

        private fun createImplObject() {
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset
            val implObjectDescriptor = loweredEnum.implObjectDescriptor
            val implObject = IrClassImpl(startOffset, endOffset, IrDeclarationOrigin.DEFINED, implObjectDescriptor)

            implObject.createParameterDeclarations()

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
                        val body = declaration.body
                        if (body is IrSyntheticBody) {
                            when (body.kind) {
                                IrSyntheticBodyKind.ENUM_VALUEOF ->
                                    declaration.body = createSyntheticValueOfMethodBody(declaration.descriptor)
                                IrSyntheticBodyKind.ENUM_VALUES ->
                                    declaration.body = createSyntheticValuesMethodBody(declaration.descriptor)
                            }
                        }
                    }
                }
                if (delete)
                    irClass.declarations.removeAt(i)
                else
                    ++i
            }

            val constructorOfAny = irClass.descriptor.module.builtIns.any.constructors.first()
            val constructor = implObjectDescriptor.createSimpleDelegatingConstructor(
                    constructorOfAny, implObjectDescriptor.constructors.single(),
                    startOffset, endOffset, DECLARATION_ORIGIN_ENUM)

            implObject.declarations.add(constructor)
            implObject.declarations.add(createSyntheticValuesPropertyDeclaration(enumEntries))

            irClass.declarations.add(implObject)
        }

        private fun createSyntheticValuesPropertyDeclaration(enumEntries: List<IrEnumEntry>): IrPropertyImpl {
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset
            val irValuesInitializer = context.createArrayOfExpression(irClass.descriptor.defaultType,
                    enumEntries.sortedBy { it.descriptor.name }.map { it.initializerExpression!! }, startOffset, endOffset)

            val irField = IrFieldImpl(startOffset, endOffset, DECLARATION_ORIGIN_ENUM,
                    loweredEnum.valuesProperty,
                    IrExpressionBodyImpl(startOffset, endOffset, irValuesInitializer))

            val getter = IrFunctionImpl(startOffset, endOffset, DECLARATION_ORIGIN_ENUM, loweredEnum.valuesGetter)

            getter.createParameterDeclarations()

            val receiver = IrGetObjectValueImpl(startOffset, endOffset,
                    loweredEnum.implObjectDescriptor.defaultType, loweredEnum.implObjectDescriptor)
            val value = IrGetFieldImpl(startOffset, endOffset, loweredEnum.valuesProperty, receiver)
            val returnStatement = IrReturnImpl(startOffset, endOffset, loweredEnum.valuesGetter, value)
            getter.body = IrBlockBodyImpl(startOffset, endOffset, listOf(returnStatement))

            val irProperty = IrPropertyImpl(startOffset, endOffset, DECLARATION_ORIGIN_ENUM,
                    false, loweredEnum.valuesProperty, irField, getter, null)
            return irProperty
        }

        private object DECLARATION_ORIGIN_ENUM :
                IrDeclarationOriginImpl("ENUM")

        private fun createSyntheticValuesMethodBody(descriptor: FunctionDescriptor): IrBody {
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset
            val valuesExpression = enumSyntheticFunctionsBuilder.buildValuesExpression(startOffset, endOffset, irClass.descriptor)

            return IrBlockBodyImpl(startOffset, endOffset,
                    listOf(IrReturnImpl(startOffset, endOffset, descriptor, valuesExpression))
            )
        }

        private fun createSyntheticValueOfMethodBody(descriptor: FunctionDescriptor): IrBody {
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset
            val value = IrGetValueImpl(startOffset, endOffset, descriptor.valueParameters[0])
            val valueOfExpression = enumSyntheticFunctionsBuilder.buildValueOfExpression(startOffset, endOffset, irClass.descriptor, value)

            return IrBlockBodyImpl(
                    startOffset, endOffset,
                    listOf(IrReturnImpl(startOffset, endOffset, descriptor, valueOfExpression))
            )
        }

        private fun lowerEnumConstructors(irClass: IrClass) {
            irClass.declarations.forEachIndexed { index, declaration ->
                if (declaration is IrConstructor)
                    irClass.declarations[index] = transformEnumConstructor(declaration)
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

            loweredEnumConstructor.createParameterDeclarations()

            enumConstructor.descriptor.valueParameters.filter { it.declaresDefaultValue() }.forEach {
                val body = enumConstructor.getDefault(it)!!
                body.transformChildrenVoid(ParameterMapper(constructorDescriptor))
                loweredEnumConstructor.putDefault(loweredConstructorDescriptor.valueParameters[it.loweredIndex()], body)
                descriptorToIrConstructorWithDefaultArguments[loweredConstructorDescriptor] = loweredEnumConstructor
            }
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
                    valueParameterDescriptor.loweredIndex()
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
                    result.putValueArgument(valueParameter.loweredIndex(), delegatingConstructorCall.getValueArgument(valueParameter))
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

private fun ValueParameterDescriptor.loweredIndex(): Int = index + 2

private class ParameterMapper(val originalDescriptor: FunctionDescriptor) : IrElementTransformerVoid() {
    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val descriptor = expression.descriptor
        when (descriptor) {
            is ValueParameterDescriptor -> {
                return IrGetValueImpl(expression.startOffset,
                        expression.endOffset,
                        originalDescriptor.valueParameters[descriptor.index])
            }
        }
        return expression
    }
}
