/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.DECLARATION_ORIGIN_ENUM
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedClassConstructorDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

internal class EnumConstructorsLowering(val context: Context) : ClassLoweringPass {

    fun run(irFile: IrFile) {
        runOnFilePostfix(irFile)
    }

    override fun lower(irClass: IrClass) {
        if (irClass.kind != ClassKind.ENUM_CLASS) return
        EnumClassTransformer(irClass).run()
    }

    private interface EnumConstructorCallTransformer {
        fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression
        fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression
    }

    private inner class EnumClassTransformer(val irClass: IrClass) {
        private val loweredEnumConstructors = mutableMapOf<IrConstructor, IrConstructor>()
        private val loweredEnumConstructorParameters = mutableMapOf<IrValueParameter, IrValueParameter>()

        fun run() {
            insertInstanceInitializerCall()
            lowerEnumConstructors(irClass)
            lowerEnumEntriesClasses()
            lowerEnumClassBody()
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
                                        irClass.symbol, context.irBuiltIns.unitType))
                            else null
                        }
                    }
                    return declaration
                }
            })
        }

        private fun lowerEnumEntriesClasses() {
            for (enumEntry in irClass.declarations.filterIsInstance<IrEnumEntry>())
                enumEntry.correspondingClass?.let { lowerEnumConstructors(it) }
        }

        private fun lowerEnumConstructors(irClass: IrClass) {
            irClass.declarations.forEachIndexed { index, declaration ->
                if (declaration is IrConstructor)
                    irClass.declarations[index] = transformEnumConstructor(declaration)
            }
        }

        private fun transformEnumConstructor(enumConstructor: IrConstructor): IrConstructor {
            val loweredEnumConstructor = lowerEnumConstructor(enumConstructor)

            for (parameter in enumConstructor.valueParameters) {
                val defaultValue = parameter.defaultValue ?: continue
                defaultValue.transformChildrenVoid(ParameterMapper(enumConstructor, loweredEnumConstructor, true))
                loweredEnumConstructor.valueParameters[parameter.loweredIndex].defaultValue = defaultValue
                defaultValue.setDeclarationsParent(loweredEnumConstructor)
            }

            return loweredEnumConstructor
        }

        private fun lowerEnumConstructor(constructor: IrConstructor): IrConstructor {
            val startOffset = constructor.startOffset
            val endOffset = constructor.endOffset
            val loweredConstructor = WrappedClassConstructorDescriptor().let {
                IrConstructorImpl(
                        startOffset, endOffset,
                        constructor.origin,
                        IrConstructorSymbolImpl(it),
                        constructor.name,
                        DescriptorVisibilities.PROTECTED,
                        constructor.returnType,
                        isInline = false,
                        isExternal = false,
                        isPrimary = constructor.isPrimary,
                        isExpect = false
                ).apply {
                    it.bind(this)
                    parent = constructor.parent
                    val body = constructor.body!!
                    this.body = body // Will be transformed later.
                    body.setDeclarationsParent(this)
                }
            }

            fun createSynthesizedValueParameter(index: Int, name: String, type: IrType): IrValueParameter =
                    WrappedValueParameterDescriptor().let {
                        IrValueParameterImpl(
                                startOffset, endOffset,
                                DECLARATION_ORIGIN_ENUM,
                                IrValueParameterSymbolImpl(it),
                                Name.identifier(name),
                                index,
                                type,
                                varargElementType = null,
                                isCrossinline = false,
                                isNoinline = false
                        ).apply {
                            it.bind(this)
                            parent = loweredConstructor
                        }
                    }

            loweredConstructor.valueParameters += createSynthesizedValueParameter(0, "name", context.irBuiltIns.stringType)
            loweredConstructor.valueParameters += createSynthesizedValueParameter(1, "ordinal", context.irBuiltIns.intType)
            loweredConstructor.valueParameters += constructor.valueParameters.map {
                it.copyTo(loweredConstructor, index = it.loweredIndex).apply {
                    loweredEnumConstructorParameters[it] = this
                }
            }

            loweredEnumConstructors[constructor] = loweredConstructor

            return loweredConstructor
        }

        private fun lowerEnumClassBody() {
            val transformer = EnumClassBodyTransformer()
            irClass.transformChildrenVoid(transformer)
            irClass.declarations.filterIsInstance<IrEnumEntry>().forEach {
                it.correspondingClass?.transformChildrenVoid(transformer)
            }
        }

        private inner class InEnumClassConstructor(val enumClassConstructor: IrConstructor) :
                EnumConstructorCallTransformer {
            override fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression {
                val startOffset = enumConstructorCall.startOffset
                val endOffset = enumConstructorCall.endOffset
                val origin = enumConstructorCall.origin

                val result = IrDelegatingConstructorCallImpl.fromSymbolDescriptor(
                        startOffset, endOffset,
                        context.irBuiltIns.unitType,
                        enumConstructorCall.symbol,
                        enumConstructorCall.symbol.owner.typeParameters.size,
                        enumConstructorCall.symbol.owner.valueParameters.size)

                assert(result.symbol.owner.valueParameters.size == 2) {
                    "Enum(String, Int) constructor call expected:\n${result.dump()}"
                }

                val nameParameter = enumClassConstructor.valueParameters.getOrElse(0) {
                    throw AssertionError("No 'name' parameter in enum constructor: $enumClassConstructor")
                }

                val ordinalParameter = enumClassConstructor.valueParameters.getOrElse(1) {
                    throw AssertionError("No 'ordinal' parameter in enum constructor: $enumClassConstructor")
                }

                result.putValueArgument(0,
                        IrGetValueImpl(startOffset, endOffset, nameParameter.type, nameParameter.symbol, origin)
                )
                result.putValueArgument(1,
                        IrGetValueImpl(startOffset, endOffset, ordinalParameter.type, ordinalParameter.symbol, origin)
                )
                return result
            }

            override fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression {
                val startOffset = delegatingConstructorCall.startOffset
                val endOffset = delegatingConstructorCall.endOffset

                val delegatingConstructor = delegatingConstructorCall.symbol.owner
                val loweredDelegatingConstructor = loweredEnumConstructors.getOrElse(delegatingConstructor) {
                    throw AssertionError("Constructor called in enum entry initializer should've been lowered: $delegatingConstructor")
                }

                val result = IrDelegatingConstructorCallImpl.fromSymbolDescriptor(
                        startOffset, endOffset,
                        context.irBuiltIns.unitType,
                        loweredDelegatingConstructor.symbol,
                        loweredDelegatingConstructor.symbol.owner.typeParameters.size,
                        loweredDelegatingConstructor.symbol.owner.valueParameters.size)

                val firstParameter = enumClassConstructor.valueParameters[0]
                result.putValueArgument(0,
                        IrGetValueImpl(startOffset, endOffset, firstParameter.type, firstParameter.symbol))
                val secondParameter = enumClassConstructor.valueParameters[1]
                result.putValueArgument(1,
                        IrGetValueImpl(startOffset, endOffset, secondParameter.type, secondParameter.symbol))

                delegatingConstructor.valueParameters.forEach {
                    result.putValueArgument(it.loweredIndex, delegatingConstructorCall.getValueArgument(it.index))
                }

                return result
            }
        }

        private abstract inner class InEnumEntry(private val enumEntry: IrEnumEntry) : EnumConstructorCallTransformer {

            override fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression {
                val name = enumEntry.name.asString()
                val ordinal = context.specialDeclarationsFactory.getEnumEntryOrdinal(enumEntry)

                val startOffset = enumConstructorCall.startOffset
                val endOffset = enumConstructorCall.endOffset

                val enumConstructor = enumConstructorCall.symbol.owner
                val loweredConstructor = loweredEnumConstructors.getOrElse(enumConstructor) {
                    throw AssertionError("Constructor called in enum entry initializer should've been lowered: $enumConstructor")
                }

                val result = createConstructorCall(startOffset, endOffset, loweredConstructor.symbol)

                result.putValueArgument(0,
                        IrConstImpl.string(startOffset, endOffset, context.irBuiltIns.stringType, name))
                result.putValueArgument(1,
                        IrConstImpl.int(startOffset, endOffset, context.irBuiltIns.intType, ordinal))

                enumConstructor.valueParameters.forEach {
                    result.putValueArgument(it.loweredIndex, enumConstructorCall.getValueArgument(it.index))
                }

                return result
            }

            override fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression {
                throw AssertionError("Unexpected delegating constructor call within enum entry: $enumEntry")
            }

            abstract fun createConstructorCall(startOffset: Int, endOffset: Int, loweredConstructor: IrConstructorSymbol): IrMemberAccessExpression<*>
        }

        private inner class InEnumEntryClassConstructor(enumEntry: IrEnumEntry) : InEnumEntry(enumEntry) {
            override fun createConstructorCall(startOffset: Int, endOffset: Int, loweredConstructor: IrConstructorSymbol) =
                    IrDelegatingConstructorCallImpl(startOffset, endOffset, context.irBuiltIns.unitType, loweredConstructor,
                    loweredConstructor.owner.typeParameters.size, loweredConstructor.owner.valueParameters.size)
        }

        private inner class InEnumEntryInitializer(enumEntry: IrEnumEntry) : InEnumEntry(enumEntry) {
            override fun createConstructorCall(startOffset: Int, endOffset: Int, loweredConstructor: IrConstructorSymbol) =
                    IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, loweredConstructor.owner.returnType, loweredConstructor)
        }

        private inner class EnumClassBodyTransformer : IrElementTransformerVoid() {
            private var enumConstructorCallTransformer: EnumConstructorCallTransformer? = null

            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.kind == ClassKind.ENUM_CLASS)
                    return declaration
                return super.visitClass(declaration)
            }

            override fun visitEnumEntry(declaration: IrEnumEntry): IrStatement {
                assert(enumConstructorCallTransformer == null) { "Nested enum entry initialization:\n${declaration.dump()}" }

                enumConstructorCallTransformer = InEnumEntryInitializer(declaration)

                declaration.initializerExpression = declaration.initializerExpression?.transform(this, data = null)

                enumConstructorCallTransformer = null

                return declaration
            }

            override fun visitConstructor(declaration: IrConstructor): IrStatement {
                val containingClass = declaration.parentAsClass

                // TODO local (non-enum) class in enum class constructor?
                val previous = enumConstructorCallTransformer

                if (containingClass.kind == ClassKind.ENUM_ENTRY) {
                    assert(enumConstructorCallTransformer == null) { "Nested enum entry initialization:\n${declaration.dump()}" }
                    val entry = irClass.declarations.filterIsInstance<IrEnumEntry>().single { it.correspondingClass == containingClass }
                    enumConstructorCallTransformer = InEnumEntryClassConstructor(entry)
                } else if (containingClass.kind == ClassKind.ENUM_CLASS) {
                    assert(enumConstructorCallTransformer == null) { "Nested enum entry initialization:\n${declaration.dump()}" }
                    enumConstructorCallTransformer = InEnumClassConstructor(declaration)
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

                if (expression.symbol.owner.parentAsClass.kind == ClassKind.ENUM_CLASS) {
                    val callTransformer = enumConstructorCallTransformer ?:
                    throw AssertionError("Enum constructor call outside of enum entry initialization or enum class constructor:\n" + irClass.dump())

                    return callTransformer.transform(expression)
                }
                return expression
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val parameter = expression.symbol.owner
                val loweredParameter = loweredEnumConstructorParameters[parameter]
                return if (loweredParameter == null) {
                    expression
                } else {
                    IrGetValueImpl(expression.startOffset, expression.endOffset, loweredParameter.type,
                            loweredParameter.symbol, expression.origin)
                }
            }

            override fun visitSetValue(expression: IrSetValue): IrExpression {
                expression.transformChildrenVoid()
                return loweredEnumConstructorParameters[expression.symbol.owner]?.let {
                    IrSetValueImpl(expression.startOffset, expression.endOffset, it.type,
                            it.symbol, expression.value, expression.origin)
                } ?: expression
            }
        }
    }
}

private val IrValueParameter.loweredIndex: Int get() = index + 2

private class ParameterMapper(superConstructor: IrConstructor,
                              val constructor: IrConstructor,
                              val useLoweredIndex: Boolean) : IrElementTransformerVoid() {
    private val valueParameters = superConstructor.valueParameters.toSet()

    override fun visitGetValue(expression: IrGetValue): IrExpression {

        val superParameter = expression.symbol.owner as? IrValueParameter ?: return expression
        if (valueParameters.contains(superParameter)) {
            val index = if (useLoweredIndex) superParameter.loweredIndex else superParameter.index
            val parameter = constructor.valueParameters[index]
            return IrGetValueImpl(
                    expression.startOffset, expression.endOffset,
                    parameter.type,
                    parameter.symbol)
        }
        return expression
    }

    override fun visitSetValue(expression: IrSetValue): IrExpression {
        expression.transformChildrenVoid()
        val superParameter = expression.symbol.owner as? IrValueParameter ?: return expression
        if (valueParameters.contains(superParameter)) {
            val index = if (useLoweredIndex) superParameter.loweredIndex else superParameter.index
            val parameter = constructor.valueParameters[index]
            return IrSetValueImpl(
                    expression.startOffset, expression.endOffset,
                    parameter.type,
                    parameter.symbol,
                    expression.value,
                    expression.origin)
        }
        return expression
    }
}
