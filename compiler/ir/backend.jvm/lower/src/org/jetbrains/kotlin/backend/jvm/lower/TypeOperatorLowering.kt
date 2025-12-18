/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.backend.jvm.ir.getKtFile
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.ir.util.getArrayElementType
import org.jetbrains.kotlin.ir.util.isBoxedArray
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isReifiedTypeParameter
import org.jetbrains.kotlin.ir.util.isSubtypeOf
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.types.Variance

/**
 * Lowers [IrTypeOperatorCall]s to (implicit) casts and instanceof checks.
 *
 * After this pass runs, there are only four kinds of [IrTypeOperatorCall]s left:
 *
 * - `IMPLICIT_CAST`
 * - `SAFE_CAST` with reified type parameters
 * - `INSTANCEOF` with non-nullable type operand or reified type parameters
 * - `CAST` with non-nullable argument, nullable type operand, or reified type parameters
 *
 * The latter two correspond to the `instanceof`/`checkcast` instructions on the JVM, except for the presence of reified type parameters.
 */
internal class TypeOperatorLowering(private val backendContext: JvmBackendContext) :
    FileLoweringPass, IrBuildingTransformer(backendContext) {

    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    private fun IrExpression.transformVoid() = transform(this@TypeOperatorLowering, null)

    private fun lowerInstanceOf(argument: IrExpression, type: IrType) = with(builder) {
        when {
            type.isReifiedTypeParameter ->
                irIs(argument, type)
            argument.type.isNullable() && type.isNullable() -> {
                irLetS(argument, irType = context.irBuiltIns.anyNType) { valueSymbol ->
                    context.oror(
                        irEqualsNull(irGet(valueSymbol.owner)),
                        irIs(irGet(valueSymbol.owner), type.makeNotNull())
                    )
                }
            }
            argument.type.isNullable() && !type.isNullable() && argument.type.erasedUpperBound == type.erasedUpperBound ->
                irNotEquals(argument, irNull())
            else -> irIs(argument, type.makeNotNull())
        }
    }

    private fun lowerCast(argument: IrExpression, type: IrType): IrExpression =
        when {
            type.isReifiedTypeParameter ->
                builder.irAs(argument, type)
            argument.type.isInlineClassType() && argument.type.isSubtypeOfClass(type.erasedUpperBound.symbol) ->
                argument
            isCompatibleArrayType(argument.type, type) ->
                argument
            type.isNullable() || argument.isDefinitelyNotNull() ->
                builder.irAs(argument, type)
            else -> {
                with(builder) {
                    irLetS(argument, irType = context.irBuiltIns.anyNType) { tmp ->
                        val message = irString("null cannot be cast to non-null type ${type.render()}")
                        if (backendContext.config.unifiedNullChecks) {
                            // Avoid branching to improve code coverage (KT-27427).
                            // We have to generate a null check here, because even if argument is of non-null type,
                            // it can be uninitialized value, which is 'null' for reference types in JMM.
                            // Most of such null checks will never actually throw, but we can't do anything about it.
                            irBlock(resultType = type) {
                                +irCall(backendContext.symbols.checkNotNullWithMessage).apply {
                                    arguments[0] = irGet(tmp.owner)
                                    arguments[1] = message
                                }
                                +irAs(irGet(tmp.owner), type.makeNullable())
                            }
                        } else {
                            irIfNull(
                                type,
                                irGet(tmp.owner),
                                irCall(throwTypeCastException).apply {
                                    arguments[0] = message
                                },
                                irAs(irGet(tmp.owner), type.makeNullable())
                            )
                        }
                    }
                }
            }
        }

    private fun isCompatibleArrayType(actualType: IrType, expectedType: IrType): Boolean {
        var actual = actualType
        var expected = expectedType
        while ((actual.isArray() || actual.isNullableArray()) && (expected.isArray() || expected.isNullableArray())) {
            actual = actual.getArrayElementLowerType()
            expected = expected.getArrayElementLowerType()
        }
        if (actual == actualType || expected == expectedType) return false
        return actual.isSubtypeOfClass(expected.erasedUpperBound.symbol)
    }

    private fun IrType.getArrayElementLowerType(): IrType =
        if (isBoxedArray && this is IrSimpleType && (arguments.singleOrNull() as? IrTypeProjection)?.variance == Variance.IN_VARIANCE)
            backendContext.irBuiltIns.anyNType
        else getArrayElementType(backendContext.irBuiltIns)

    // TODO extract null check elimination on IR somewhere?
    private fun IrExpression.isDefinitelyNotNull(): Boolean =
        when (this) {
            is IrGetValue ->
                this.symbol.owner.isDefinitelyNotNullVal()
            is IrGetClass,
            is IrConstructorCall ->
                true
            is IrCall ->
                this.symbol == backendContext.irBuiltIns.checkNotNullSymbol
            else ->
                false
        }

    private fun IrValueDeclaration.isDefinitelyNotNullVal(): Boolean {
        val irVariable = this as? IrVariable ?: return false
        return !irVariable.isVar && irVariable.initializer?.isDefinitelyNotNull() == true
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression = with(builder) {
        at(expression)
        return when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT ->
                irComposite(resultType = expression.type) {
                    +expression.argument.transformVoid()
                    // TODO: Don't generate these casts in the first place
                    if (!expression.argument.type.isSubtypeOf(expression.type.makeNullable(), backendContext.typeSystem)) {
                        +IrCompositeImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, expression.type)
                    }
                }

            // There is no difference between IMPLICIT_CAST and IMPLICIT_INTEGER_COERCION on JVM_IR.
            // Instead, this is handled in StackValue.coerce.
            IrTypeOperator.IMPLICIT_INTEGER_COERCION ->
                irImplicitCast(expression.argument.transformVoid(), expression.typeOperand)

            IrTypeOperator.CAST ->
                lowerCast(expression.argument.transformVoid(), expression.typeOperand)

            IrTypeOperator.SAFE_CAST ->
                if (expression.typeOperand.isReifiedTypeParameter) {
                    expression.transformChildrenVoid()
                    expression
                } else {
                    irLetS(
                        expression.argument.transformVoid(),
                        IrStatementOrigin.SAFE_CALL,
                        irType = context.irBuiltIns.anyNType
                    ) { valueSymbol ->
                        val thenPart =
                            if (valueSymbol.owner.type.isInlineClassType())
                                lowerCast(irGet(valueSymbol.owner), expression.typeOperand)
                            else
                                irGet(valueSymbol.owner)
                        irIfThenElse(
                            expression.type,
                            lowerInstanceOf(irGet(valueSymbol.owner), expression.typeOperand.makeNotNull()),
                            thenPart,
                            irNull(expression.type)
                        )
                    }
                }

            IrTypeOperator.INSTANCEOF ->
                lowerInstanceOf(expression.argument.transformVoid(), expression.typeOperand)

            IrTypeOperator.NOT_INSTANCEOF ->
                irNot(lowerInstanceOf(expression.argument.transformVoid(), expression.typeOperand))

            IrTypeOperator.IMPLICIT_NOTNULL -> {
                val text = computeNotNullAssertionText(expression)

                irLetS(expression.argument.transformVoid(), irType = context.irBuiltIns.anyNType) { valueSymbol ->
                    irComposite(resultType = expression.type) {
                        if (text != null) {
                            +irCall(checkExpressionValueIsNotNull).apply {
                                arguments[0] = irGet(valueSymbol.owner)
                                arguments[1] = irString(text.trimForRuntimeAssertion())
                            }
                        } else {
                            +irCall(backendContext.symbols.checkNotNull).apply {
                                arguments[0] = irGet(valueSymbol.owner)
                            }
                        }
                        +irGet(valueSymbol.owner)
                    }
                }
            }

            else -> {
                expression.transformChildrenVoid()
                expression
            }
        }
    }

    private fun IrBuilderWithScope.computeNotNullAssertionText(typeOperatorCall: IrTypeOperatorCall): String? {
        if (backendContext.config.noSourceCodeInNotNullAssertionExceptions) {
            return when (val argument = typeOperatorCall.argument) {
                is IrCall -> "${argument.symbol.owner.name.asString()}(...)"
                is IrGetField -> {
                    val field = argument.symbol.owner
                    field.name.asString().takeUnless { field.origin.isSynthetic }
                }
                else -> null
            }
        }

        val owner = scope.scopeOwnerSymbol.owner
        if (owner is IrFunction && owner.isDelegated())
            return "${owner.name.asString()}(...)"

        val declarationParent = parent as? IrDeclaration
        val sourceView = declarationParent?.let(::sourceViewFor)
        val (startOffset, endOffset) = typeOperatorCall.extents()
        return if (sourceView?.validSourcePosition(startOffset, endOffset) == true) {
            sourceView.subSequence(startOffset, endOffset).toString()
        } else {
            // Fallback for inconsistent line numbers
            (declarationParent as? IrDeclarationWithName)?.name?.asString() ?: "Unknown Declaration"
        }
    }

    private fun String.trimForRuntimeAssertion() = StringUtil.trimMiddle(this, 50)

    private fun IrFunction.isDelegated() =
        origin == IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR ||
                origin == IrDeclarationOrigin.DELEGATED_MEMBER

    private fun CharSequence.validSourcePosition(startOffset: Int, endOffset: Int): Boolean =
        startOffset in 0 until endOffset && endOffset < length

    private fun IrElement.extents(): Pair<Int, Int> {
        var startOffset = Int.MAX_VALUE
        var endOffset = 0
        acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
                if (element.startOffset in 0 until startOffset)
                    startOffset = element.startOffset
                if (endOffset < element.endOffset)
                    endOffset = element.endOffset
            }
        })
        return startOffset to endOffset
    }

    private fun sourceViewFor(declaration: IrDeclaration): CharSequence? =
        declaration.fileParent.getKtFile()?.viewProvider?.contents

    private val throwTypeCastException: IrSimpleFunctionSymbol =
        backendContext.symbols.throwTypeCastException

    private val checkExpressionValueIsNotNull: IrSimpleFunctionSymbol =
        if (backendContext.config.unifiedNullChecks)
            backendContext.symbols.checkNotNullExpressionValue
        else
            backendContext.symbols.checkExpressionValueIsNotNull
}
