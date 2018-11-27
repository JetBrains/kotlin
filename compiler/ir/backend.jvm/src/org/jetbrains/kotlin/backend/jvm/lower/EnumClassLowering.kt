/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import gnu.trove.TObjectIntHashMap
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.common.descriptors.WrappedClassConstructorDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedFieldDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.*
import java.util.*

internal val enumClassPhase = makeIrFilePhase(
    ::EnumClassLowering,
    name = "EnumClass",
    description = "Handle enum classes"
)

private class EnumClassLowering(val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        if (!irClass.isEnumClass) return

        EnumClassTransformer(irClass).run()
    }

    private interface EnumConstructorCallTransformer {
        fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression
        fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression
    }

    private fun createArrayOfExpression(arrayElementType: IrSimpleType, arrayElements: List<IrExpression>): IrExpression {
        val arrayOfFun = context.ir.symbols.arrayOf.owner

        /* Substituted descriptor is only needed to construct IrCall */
        val unsubstitutedArrayOfFunDescriptor = arrayOfFun.descriptor
        val typeParameter0 = unsubstitutedArrayOfFunDescriptor.typeParameters[0]
        val typeSubstitutor = TypeSubstitutor.create(
            mapOf(typeParameter0.typeConstructor to TypeProjectionImpl(arrayElementType.toKotlinType()))
        )
        val substitutedArrayOfDescriptor = unsubstitutedArrayOfFunDescriptor.substitute(typeSubstitutor)!!

        val arrayType = context.irBuiltIns.arrayClass.typeWith(arrayElementType)
        val arg0 =
            IrVarargImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, arrayType, arrayElementType, arrayElements)

        return IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            arrayType,
            arrayOfFun.symbol,
            substitutedArrayOfDescriptor,
            arrayOfFun.typeParameters.size
        ).apply {
            putTypeArgument(0, arrayElementType)
            putValueArgument(0, arg0)
        }
    }

    private inner class EnumClassTransformer(val irClass: IrClass) {
        private val enumEntryOrdinals = TObjectIntHashMap<IrEnumEntry>()
        private val enumEntryClassToEntry = HashMap<IrClass, IrEnumEntry>()
        private val loweredEnumConstructors = HashMap<IrConstructorSymbol, IrConstructorImpl>()
        private val loweredEnumConstructorParameters = HashMap<IrValueParameterSymbol, IrValueParameter>()
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
            val descriptor = WrappedClassConstructorDescriptor(enumConstructor.descriptor.annotations, enumConstructor.descriptor.source)
            return IrConstructorImpl(
                enumConstructor.startOffset, enumConstructor.endOffset,
                enumConstructor.origin,
                IrConstructorSymbolImpl(descriptor),
                enumConstructor.name,
                Visibilities.PROTECTED,
                returnType = enumConstructor.returnType,
                isInline = enumConstructor.isInline,
                isExternal = enumConstructor.isExternal,
                isPrimary = enumConstructor.isPrimary
            ).apply {
                val newConstructor = this
                descriptor.bind(this)
                parent = enumClass

                val nameParameter = makeNameValueParameter(newConstructor)
                val ordinalParameter = makeOrdinalValueParameter(newConstructor)
                valueParameters.add(0, nameParameter)
                valueParameters.add(1, ordinalParameter)
                valueParameters.addAll(enumConstructor.valueParameters.map { param ->
                    param.copyTo(newConstructor, index = param.index + 2).also { newParam ->
                        loweredEnumConstructorParameters[param.symbol] = newParam
                    }
                })

                body = enumConstructor.body // will be transformed later

                loweredEnumConstructors[enumConstructor.symbol] = this
                metadata = enumConstructor.metadata
            }
        }

        private fun makeNameValueParameter(constructor: IrConstructor): IrValueParameter {
            val descriptor = WrappedValueParameterDescriptor()
            return IrValueParameterImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                IrDeclarationOrigin.DEFINED,
                IrValueParameterSymbolImpl(descriptor),
                Name.identifier("name"),
                index = 0,
                type = context.irBuiltIns.stringType,
                varargElementType = null,
                isCrossinline = false,
                isNoinline = false
            ).apply {
                descriptor.bind(this)
                parent = constructor
            }
        }

        private fun makeOrdinalValueParameter(constructor: IrConstructor): IrValueParameter {
            val descriptor = WrappedValueParameterDescriptor()
            return IrValueParameterImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                IrDeclarationOrigin.DEFINED,
                IrValueParameterSymbolImpl(descriptor),
                Name.identifier("ordinal"),
                index = 1,
                type = context.irBuiltIns.intType,
                varargElementType = null,
                isCrossinline = false,
                isNoinline = false
            ).apply {
                descriptor.bind(this)
                parent = constructor
            }
        }

        private fun lowerEnumEntries() {
            irClass.transformDeclarationsFlat { declaration ->
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

        private fun createFieldForEnumEntry(enumEntry: IrEnumEntry): IrField =
            context.declarationFactory.getFieldForEnumEntry(
                enumEntry, (enumEntry.correspondingClass ?: enumEntry.parentAsClass).defaultType
            ).also {
                it.initializer = IrExpressionBodyImpl(enumEntry.initializerExpression!!)
                enumEntryFields.add(it)
                enumEntriesByField[it] = enumEntry
            }

        private fun setupSynthesizedEnumClassMembers() {
            val irField = createSyntheticValuesFieldDeclaration()

            irClass.declarations.add(irField)

            valuesFunction = findFunctionForMemberWithSyntheticBodyKind(IrSyntheticBodyKind.ENUM_VALUES)
            valueOfFunction = findFunctionForMemberWithSyntheticBodyKind(IrSyntheticBodyKind.ENUM_VALUEOF)
        }

        private fun findFunctionForMemberWithSyntheticBodyKind(kind: IrSyntheticBodyKind): IrFunction =
            irClass.declarations.asSequence().filterIsInstance<IrFunction>()
                .first {
                    it.body.let { body ->
                        body is IrSyntheticBody && body.kind == kind
                    }
                }


        private fun createSyntheticValuesFieldDeclaration(): IrFieldImpl {
            val valuesArrayType = context.irBuiltIns.arrayClass.typeWith(irClass.defaultType)

            val irValuesInitializer = createSyntheticValuesFieldInitializerExpression()

            val descriptor = WrappedFieldDescriptor()
            // TODO: mark ACC_SYNTHETIC
            return IrFieldImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.FIELD_FOR_ENUM_VALUES,
                IrFieldSymbolImpl(descriptor),
                Name.identifier("\$VALUES"),
                valuesArrayType,
                Visibilities.PRIVATE,
                isFinal = true,
                isExternal = false,
                isStatic = true
            ).also {
                descriptor.bind(it)
                it.parent = irClass
                it.initializer = IrExpressionBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irValuesInitializer)

                valuesField = it
            }
        }

        private fun createSyntheticValuesFieldInitializerExpression(): IrExpression =
            createArrayOfExpression(
                irClass.defaultType,
                enumEntryFields.map { irField ->
                    IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irField.symbol, irField.symbol.owner.type)
                })

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

                assert(enumConstructorCall.typeArgumentsCount == 1) { "Enum<T> call expected:\n${result.dump()}" }
                result.putTypeArgument(0, enumConstructorCall.getTypeArgument(0))

                assert(result.symbol.owner.valueParameters.size == 2) {
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
                val startOffset = delegatingConstructorCall.startOffset
                val endOffset = delegatingConstructorCall.endOffset

                val loweredDelegatedConstructor = loweredEnumConstructors.getOrElse(delegatingConstructorCall.symbol) {
                    throw AssertionError("Constructor called in enum entry initializer should've been lowered: ${delegatingConstructorCall.symbol}")
                }

                val result = IrDelegatingConstructorCallImpl(
                    startOffset,
                    endOffset,
                    context.irBuiltIns.unitType,
                    loweredDelegatedConstructor.symbol,
                    loweredDelegatedConstructor.descriptor,
                    loweredDelegatedConstructor.typeParameters.size
                )

                assert(loweredDelegatedConstructor.typeParameters.size == 0) { "Enum delegating call expected:\n${result.dump()}" }

                result.putValueArgument(0, IrGetValueImpl(startOffset, endOffset, irEnumConstructor.valueParameters[0].symbol))
                result.putValueArgument(1, IrGetValueImpl(startOffset, endOffset, irEnumConstructor.valueParameters[1].symbol))

                delegatingConstructorCall.symbol.owner.valueParameters.forEach { valueParameter ->
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

                val startOffset = enumConstructorCall.startOffset
                val endOffset = enumConstructorCall.endOffset

                val loweredConstructor = loweredEnumConstructors.getOrElse(enumConstructorCall.symbol) {
                    throw AssertionError("Constructor called in enum entry initializer should've been lowered:\n${enumConstructorCall.dump()}")
                }

                val result = createConstructorCall(startOffset, endOffset, loweredConstructor)

                result.putValueArgument(0, IrConstImpl.string(startOffset, endOffset, context.irBuiltIns.stringType, name))
                result.putValueArgument(1, IrConstImpl.int(startOffset, endOffset, context.irBuiltIns.intType, ordinal))

                enumConstructorCall.symbol.owner.valueParameters.forEach { valueParameter ->
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

                if (expression.symbol.owner.parentAsClass.kind == ClassKind.ENUM_CLASS) {
                    val callTransformer = enumConstructorCallTransformer ?: throw AssertionError(
                        "Enum constructor call outside of enum entry initialization or enum class constructor:\n" +
                                irClass.dump()
                    )

                    return callTransformer.transform(expression)
                }

                return expression
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val loweredParameter = loweredEnumConstructorParameters[expression.symbol]
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
                val enumValueOf = context.irBuiltIns.enumValueOfFun
                val returnType = irClass.defaultType

                val unsubstitutedValueOfDescriptor = enumValueOf.descriptor
                val typeParameter0 = unsubstitutedValueOfDescriptor.typeParameters[0]
                val enumClassType = irClass.descriptor.defaultType
                val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameter0.typeConstructor to TypeProjectionImpl(enumClassType)))
                val substitutedValueOfDescriptor = unsubstitutedValueOfDescriptor.substitute(typeSubstitutor)!!

                val irValueOfCall =
                    IrCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        returnType,
                        enumValueOf.symbol,
                        substitutedValueOfDescriptor,
                        enumValueOf.typeParameters.size
                    )
                irValueOfCall.putTypeArgument(0, irClass.defaultType)
                irValueOfCall.putValueArgument(
                    0, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, valueOfFunction.valueParameters[0].symbol)
                )

                return IrBlockBodyImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    listOf(
                        IrReturnImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            returnType,
                            valueOfFunction.symbol,
                            irValueOfCall
                        )
                    )
                )
            }

            private fun createEnumValuesBody(valuesField: IrField): IrBody {
                val cloneFun = context.irBuiltIns.arrayClass.owner.functions.find { it.name.asString() == "clone" }!!

                val irCloneValues =
                    IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, cloneFun.returnType, cloneFun.symbol, cloneFun.descriptor, 0).apply {
                        dispatchReceiver =
                                IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, valuesField.symbol, valuesField.symbol.owner.type)
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
