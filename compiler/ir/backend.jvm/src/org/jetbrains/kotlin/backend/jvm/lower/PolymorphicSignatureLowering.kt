/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.jvm.checkers.PolymorphicSignatureCallChecker

internal val polymorphicSignaturePhase = makeIrFilePhase(
    ::PolymorphicSignatureLowering,
    name = "PolymorphicSignature",
    description = "Replace polymorphic methods with fake ones according to types at the call site"
)

class PolymorphicSignatureLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        if (context.state.languageVersionSettings.supportsFeature(LanguageFeature.PolymorphicSignature))
            irFile.transformChildrenVoid()
    }

    private fun IrTypeOperatorCall.isCast(): Boolean =
        operator != IrTypeOperator.INSTANCEOF && operator != IrTypeOperator.NOT_INSTANCEOF

    // If the return type is Any?, then it is also polymorphic (e.g. MethodHandle.invokeExact
    // has polymorphic return type, while VarHandle.compareAndSet does not).
    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression =
        (expression.argument as? IrCall)?.takeIf { expression.isCast() && it.type.isNullableAny() }?.transform(expression.typeOperand)
            ?: super.visitTypeOperator(expression)

    override fun visitCall(expression: IrCall): IrExpression =
        expression.transform(null) ?: super.visitCall(expression)

    private fun IrCall.transform(castReturnType: IrType?): IrCall? {
        val function = symbol.owner as? IrSimpleFunction ?: return null
        if (!function.hasAnnotation(PolymorphicSignatureCallChecker.polymorphicSignatureFqName))
            return null
        assert(function.valueParameters.singleOrNull()?.varargElementType != null) {
            "@PolymorphicSignature methods should only have a single vararg argument: ${dump()}"
        }

        val values = (getValueArgument(0) as IrVararg?)?.elements?.map {
            when (it) {
                is IrExpression -> it
                is IrSpreadElement -> it.expression // `*xs` acts as `xs` (for compatibility?)
                else -> throw AssertionError("unknown IrVarargElement: $it")
            }
        } ?: listOf()
        val fakeFunction = buildFun {
            updateFrom(function)
            name = function.name
            origin = JvmLoweredDeclarationOrigin.POLYMORPHIC_SIGNATURE_INSTANTIATION
            returnType = castReturnType ?: function.returnType
        }.apply {
            parent = function.parent
            copyTypeParametersFrom(function)
            dispatchReceiverParameter = function.dispatchReceiverParameter
            extensionReceiverParameter = function.extensionReceiverParameter
            for ((i, value) in values.withIndex()) {
                addValueParameter("\$$i", value.type, JvmLoweredDeclarationOrigin.POLYMORPHIC_SIGNATURE_INSTANTIATION)
            }
        }
        return IrCallImpl(startOffset, endOffset, fakeFunction.returnType, fakeFunction.symbol, origin, superQualifierSymbol).apply {
            copyTypeArgumentsFrom(this@transform)
            dispatchReceiver = this@transform.dispatchReceiver
            extensionReceiver = this@transform.extensionReceiver
            values.forEachIndexed(::putValueArgument)
            transformChildrenVoid()
        }
    }
}
