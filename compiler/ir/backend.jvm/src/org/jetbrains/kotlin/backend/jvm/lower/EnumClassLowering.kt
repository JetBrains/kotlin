/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import gnu.trove.TObjectIntHashMap
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmPropertyDescriptorImpl
import org.jetbrains.kotlin.backend.jvm.descriptors.createValueParameter
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.types.*
import org.jetbrains.org.objectweb.asm.Opcodes
import java.util.*

class EnumClassLowering(val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        if (!irClass.isEnumClass) return

        EnumClassTransformer(irClass).run()
    }

    private interface EnumConstructorCallTransformer {
        fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression
        fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression
    }

    private val unsubstitutedArrayOfFun = context.ir.symbols.arrayOf.descriptor

    private fun createArrayOfExpression(arrayElementType: KotlinType, arrayElements: List<IrExpression>): IrExpression {
        val typeParameter0 = unsubstitutedArrayOfFun.typeParameters[0]
        val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameter0.typeConstructor to TypeProjectionImpl(arrayElementType)))
        val substitutedArrayOfFun = unsubstitutedArrayOfFun.substitute(typeSubstitutor)!!


        val valueParameter0 = substitutedArrayOfFun.valueParameters[0]
        val arg0VarargType = valueParameter0.type
        val arg0VarargElementType = valueParameter0.varargElementType!!
        val arg0 =
            IrVarargImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, arg0VarargType.toIrType()!!, arg0VarargElementType.toIrType()!!, arrayElements)

        return IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            substitutedArrayOfFun.returnType!!.toIrType()!!,
            substitutedArrayOfFun,
            substitutedArrayOfFun.typeParametersCount
        ).apply {
            putValueArgument(0, arg0)
        }
    }

    private inner class EnumClassTransformer(val irClass: IrClass) {
        private val enumEntryOrdinals = TObjectIntHashMap<IrEnumEntry>()
        private val enumEntryClassToEntry = HashMap<IrClass, IrEnumEntry>()
        private val loweredEnumConstructors = HashMap<ClassConstructorDescriptor, IrConstructorImpl>()
        private val loweredEnumConstructorParameters = HashMap<ValueParameterDescriptor, IrValueParameter>()
        private val enumEntriesByField = HashMap<IrField, IrEnumEntry>()
        private val enumEntryFields = ArrayList<IrField>()

        private lateinit var valuesField: IrField
        private lateinit var valuesFunction: IrFunction
        private lateinit var valueOfFunction: IrFunction

        fun run() {
            assignOrdinalsToEnumEntries()
            lowerEnumConstructors(irClass)
            lowerEnumEntries()
            setupSynthesizedEnumClassMembers()
            lowerEnumClassBody()
        }

        private fun assignOrdinalsToEnumEntries() {
            var ordinal = 0
            irClass.declarations.forEach {
                if (it is IrEnumEntry) {
                    enumEntryOrdinals.put(it, ordinal)
                    it.correspondingClass?.run {
                        enumEntryClassToEntry.put(this, it)
                    }
                    ordinal++
                }
            }
        }

        private fun lowerEnumConstructors(irClass: IrClass) {
            irClass.declarations.transform { declaration ->
                if (declaration is IrConstructor)
                    transformEnumConstructor(declaration, irClass)
                else
                    declaration
            }
        }

        private fun transformEnumConstructor(
            enumConstructor: IrConstructor,
            enumClass: IrClass
        ): IrConstructor {
            val constructorDescriptor = enumConstructor.descriptor
            val loweredConstructorDescriptor = lowerEnumConstructor(constructorDescriptor)
            return IrConstructorImpl(
                enumConstructor.startOffset, enumConstructor.endOffset, enumConstructor.origin,
                loweredConstructorDescriptor,
                enumConstructor.body!! // will be transformed later
            ).apply {
                parent = enumClass
                returnType = loweredConstructorDescriptor.returnType.toIrType()!!
                createParameterDeclarations()
                loweredEnumConstructors[constructorDescriptor] = this
                constructorDescriptor.valueParameters.forEach {
                    loweredEnumConstructorParameters[it] = valueParameters[2 + it.index]
                }
            }
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

            return loweredConstructorDescriptor
        }

        private fun lowerConstructorValueParameter(
            loweredConstructorDescriptor: ClassConstructorDescriptor,
            valueParameterDescriptor: ValueParameterDescriptor
        ): ValueParameterDescriptor {
            return valueParameterDescriptor.copy(
                loweredConstructorDescriptor,
                valueParameterDescriptor.name,
                valueParameterDescriptor.index + 2
            )
        }

        private fun lowerEnumEntries() {
            irClass.declarations.transformFlat { declaration ->
                if (declaration is IrEnumEntry) {
                    listOfNotNull(
                        createFieldForEnumEntry(declaration),
                        lowerEnumEntryClass(declaration.correspondingClass)
                    )
                } else null
            }
        }

        private fun lowerEnumEntryClass(enumEntryClass: IrClass?): IrClass? {
            if (enumEntryClass == null) return null

            lowerEnumConstructors(enumEntryClass)

            return enumEntryClass
        }

        private fun createFieldForEnumEntry(enumEntry: IrEnumEntry) =
            context.declarationFactory.getFieldForEnumEntry(enumEntry, enumEntry.initializerExpression!!.type).also {
                it.initializer = IrExpressionBodyImpl(enumEntry.initializerExpression!!)
                enumEntryFields.add(it)
                enumEntriesByField[it] = enumEntry
            }

        private fun setupSynthesizedEnumClassMembers() {
            val irField = createSyntheticValuesFieldDeclaration()

            irClass.declarations.add(irField)

            valuesFunction = findFunctionDescriptorForMemberWithSyntheticBodyKind(IrSyntheticBodyKind.ENUM_VALUES)
            valueOfFunction = findFunctionDescriptorForMemberWithSyntheticBodyKind(IrSyntheticBodyKind.ENUM_VALUEOF)
        }

        private fun findFunctionDescriptorForMemberWithSyntheticBodyKind(kind: IrSyntheticBodyKind): IrFunction =
            irClass.declarations.asSequence().filterIsInstance<IrFunction>()
                .first {
                    it.body.let { body ->
                        body is IrSyntheticBody && body.kind == kind
                    }
                }


        private fun createSyntheticValuesFieldDeclaration(): IrFieldImpl {
            val valuesArrayType = context.builtIns.getArrayType(Variance.INVARIANT, irClass.descriptor.defaultType)

            val irValuesInitializer = createSyntheticValuesFieldInitializerExpression()

            return IrFieldImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, JvmLoweredDeclarationOrigin.FIELD_FOR_ENUM_VALUES,
                createSyntheticValuesFieldDescriptor(valuesArrayType),
                valuesArrayType.toIrType()!!,
                IrExpressionBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irValuesInitializer)
            ).also { valuesField = it }
        }

        private fun createSyntheticValuesFieldInitializerExpression(): IrExpression =
            createArrayOfExpression(
                irClass.defaultType.toKotlinType(),
                enumEntryFields.map { irField ->
                    IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irField.symbol, irField.symbol.owner.type)
                })

        private fun createSyntheticValuesFieldDescriptor(valuesArrayType: SimpleType): PropertyDescriptorImpl {
            return JvmPropertyDescriptorImpl.createStaticVal(
                Name.identifier("\$VALUES"),
                valuesArrayType,
                irClass.descriptor,
                Annotations.EMPTY,
                Modality.FINAL, Visibilities.PRIVATE, Opcodes.ACC_SYNTHETIC,
                irClass.descriptor.source
            )
        }

        private fun lowerEnumClassBody() {
            irClass.transformChildrenVoid(EnumClassBodyTransformer())
        }

        private inner class InEnumClassConstructor(val irEnumConstructor: IrConstructor) :
            EnumConstructorCallTransformer {
            override fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression {
                val startOffset = enumConstructorCall.startOffset
                val endOffset = enumConstructorCall.endOffset
                val origin = enumConstructorCall.origin

                val result = IrDelegatingConstructorCallImpl(
                    startOffset,
                    endOffset,
                    context.irBuiltIns.unitType,
                    enumConstructorCall.symbol,
                    enumConstructorCall.descriptor,
                    enumConstructorCall.typeArgumentsCount
                )

                assert(result.descriptor.valueParameters.size == 2) {
                    "Enum(String, Int) constructor call expected:\n${result.dump()}"
                }

                val nameParameter = irEnumConstructor.valueParameters.getOrElse(0) {
                    throw AssertionError("No 'name' parameter in enum constructor: ${irEnumConstructor.dump()}")
                }

                val ordinalParameter = irEnumConstructor.valueParameters.getOrElse(1) {
                    throw AssertionError("No 'ordinal' parameter in enum constructor: ${irEnumConstructor.dump()}")
                }

                result.putValueArgument(0, IrGetValueImpl(startOffset, endOffset, nameParameter.symbol, origin))
                result.putValueArgument(1, IrGetValueImpl(startOffset, endOffset, ordinalParameter.symbol, origin))

                return result
            }

            override fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression {
                val descriptor = delegatingConstructorCall.descriptor
                val startOffset = delegatingConstructorCall.startOffset
                val endOffset = delegatingConstructorCall.endOffset

                val loweredDelegatedConstructor = loweredEnumConstructors.getOrElse(descriptor) {
                    throw AssertionError("Constructor called in enum entry initializer should've been lowered: $descriptor")
                }

                val result = IrDelegatingConstructorCallImpl(
                    startOffset,
                    endOffset,
                    context.irBuiltIns.unitType,
                    loweredDelegatedConstructor.symbol,
                    loweredDelegatedConstructor.descriptor,
                    loweredDelegatedConstructor.typeParameters.size
                )

                result.putValueArgument(0, IrGetValueImpl(startOffset, endOffset, irEnumConstructor.valueParameters[0].symbol))
                result.putValueArgument(1, IrGetValueImpl(startOffset, endOffset, irEnumConstructor.valueParameters[1].symbol))

                descriptor.valueParameters.forEach { valueParameter ->
                    val i = valueParameter.index
                    result.putValueArgument(i + 2, delegatingConstructorCall.getValueArgument(i))
                }

                return result
            }
        }

        private abstract inner class InEnumEntry(private val enumEntry: IrEnumEntry) : EnumConstructorCallTransformer {
            override fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression {
                val name = enumEntry.name.asString()
                val ordinal = enumEntryOrdinals[enumEntry]

                val descriptor = enumConstructorCall.descriptor
                val startOffset = enumConstructorCall.startOffset
                val endOffset = enumConstructorCall.endOffset

                val loweredConstructor = loweredEnumConstructors.getOrElse(descriptor) {
                    throw AssertionError("Constructor called in enum entry initializer should've been lowered: $descriptor")
                }

                val result = createConstructorCall(startOffset, endOffset, loweredConstructor)

                result.putValueArgument(0, IrConstImpl.string(startOffset, endOffset, context.irBuiltIns.stringType, name))
                result.putValueArgument(1, IrConstImpl.int(startOffset, endOffset, context.irBuiltIns.intType, ordinal))

                descriptor.valueParameters.forEach { valueParameter ->
                    val i = valueParameter.index
                    result.putValueArgument(i + 2, enumConstructorCall.getValueArgument(i))
                }

                return result
            }

            override fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression {
                throw AssertionError("Unexpected delegating constructor call within enum entry: $enumEntry")
            }

            abstract fun createConstructorCall(
                startOffset: Int,
                endOffset: Int,
                loweredConstructor: IrConstructor
            ): IrMemberAccessExpression
        }

        private inner class InEnumEntryClassConstructor(enumEntry: IrEnumEntry) : InEnumEntry(enumEntry) {
            override fun createConstructorCall(startOffset: Int, endOffset: Int, loweredConstructor: IrConstructor) =
                IrDelegatingConstructorCallImpl(
                    startOffset,
                    endOffset,
                    context.irBuiltIns.unitType,
                    loweredConstructor.symbol,
                    loweredConstructor.descriptor,
                    loweredConstructor.typeParameters.size
                )
        }

        private inner class InEnumEntryInitializer(enumEntry: IrEnumEntry) : InEnumEntry(enumEntry) {
            override fun createConstructorCall(startOffset: Int, endOffset: Int, loweredConstructor: IrConstructor) =
                IrCallImpl(
                    startOffset,
                    endOffset,
                    loweredConstructor.symbol.owner.parentAsClass.defaultType,
                    loweredConstructor.symbol
                )
        }

        private inner class EnumClassBodyTransformer : IrElementTransformerVoid() {
            private var enumConstructorCallTransformer: EnumConstructorCallTransformer? = null

            override fun visitField(declaration: IrField): IrStatement {
                val enumEntry = enumEntriesByField[declaration]
                if (enumEntry == null) {
                    declaration.transformChildrenVoid(this)
                    return declaration
                }

                assert(enumConstructorCallTransformer == null) { "Nested enum entry initialization:\n${declaration.dump()}" }

                enumConstructorCallTransformer = InEnumEntryInitializer(enumEntry)

                val result = super.visitField(declaration)

                enumConstructorCallTransformer = null

                return result
            }

            override fun visitConstructor(declaration: IrConstructor): IrStatement {
                val containingClass = declaration.parent as IrClass

                // TODO local (non-enum) class in enum class constructor?
                val previous = enumConstructorCallTransformer

                if (containingClass.isEnumEntry) {
                    assert(enumConstructorCallTransformer == null) { "Nested enum entry initialization:\n${declaration.dump()}" }
                    enumConstructorCallTransformer = InEnumEntryClassConstructor(enumEntryClassToEntry[containingClass]!!)
                } else if (containingClass.isEnumClass) {
                    assert(enumConstructorCallTransformer == null) { "Nested enum entry initialization:\n${declaration.dump()}" }
                    enumConstructorCallTransformer = InEnumClassConstructor(declaration)
                }

                val result = super.visitConstructor(declaration)

                enumConstructorCallTransformer = previous

                return result
            }

            override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                val callTransformer = enumConstructorCallTransformer ?: throw AssertionError(
                    "Enum constructor call outside of enum entry initialization or enum class constructor:\n" +
                            irClass.dump()
                )

                return callTransformer.transform(expression)
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                if (expression.descriptor.containingDeclaration.kind == ClassKind.ENUM_CLASS) {
                    val callTransformer = enumConstructorCallTransformer ?: throw AssertionError(
                        "Enum constructor call outside of enum entry initialization or enum class constructor:\n" +
                                irClass.dump()
                    )

                    return callTransformer.transform(expression)
                }

                return expression
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val loweredParameter = loweredEnumConstructorParameters[expression.descriptor]
                return if (loweredParameter != null) {
                    IrGetValueImpl(expression.startOffset, expression.endOffset, loweredParameter.symbol, expression.origin)
                } else {
                    expression
                }
            }

            override fun visitSyntheticBody(body: IrSyntheticBody): IrBody {
                return when (body.kind) {
                    IrSyntheticBodyKind.ENUM_VALUES ->
                        createEnumValuesBody(valuesField)
                    IrSyntheticBodyKind.ENUM_VALUEOF ->
                        createEnumValueOfBody()
                    else ->
                        body
                }
            }

            private fun createEnumValueOfBody(): IrBody {
                val unsubstitutedValueOf = context.irBuiltIns.enumValueOf
                val typeParameterT = unsubstitutedValueOf.typeParameters[0]
                val enumClassType = irClass.descriptor.defaultType
                val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameterT.typeConstructor to TypeProjectionImpl(enumClassType)))
                val substitutedValueOf = unsubstitutedValueOf.substitute(typeSubstitutor)!!

                val irValueOfCall =
                    IrCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        substitutedValueOf.returnType!!.toIrType()!!,
                        substitutedValueOf,
                        substitutedValueOf.typeParametersCount
                    )
                irValueOfCall.putValueArgument(
                    0, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, valueOfFunction.valueParameters[0].symbol)
                )

                return IrBlockBodyImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    listOf(
                        IrReturnImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            valueOfFunction.returnType,
                            valueOfFunction.symbol,
                            irValueOfCall
                        )
                    )
                )
            }

            private fun createEnumValuesBody(valuesField: IrField): IrBody {
                val cloneFun = valuesField.type.toKotlinType().memberScope.findSingleFunction(Name.identifier("clone"))

                val irCloneValues = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, cloneFun.returnType!!.toIrType()!!, cloneFun, 0).apply {
                    dispatchReceiver = IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, valuesField.symbol, valuesField.symbol.owner.type)
                }

                return IrBlockBodyImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    listOf(
                        IrReturnImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            valuesFunction.symbol.owner.returnType,
                            valuesFunction.symbol,
                            irCloneValues
                        )
                    )
                )
            }
        }
    }


}