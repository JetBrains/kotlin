/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredStatementOrigin
import org.jetbrains.kotlin.backend.jvm.ir.isInlineFunctionCall
import org.jetbrains.kotlin.codegen.AsmUtil.RECEIVER_PARAMETER_NAME
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irRichFunctionReference
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isInlineParameter
import org.jetbrains.kotlin.ir.util.isInlineSuspendParameter
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideOrSelf
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Sets a proper origin for inlinable function references.
 * For inlinable function references, first converts it into a function reference, then sets the origin.
 *
 * Normally, the proper origin is `INLINE_LAMBDA`.
 * Default values of inlinable parameters are marked with a special `DEFAULT_VALUE_OF_INLINABLE_PARAMETER` origin, in order to
 * generate explicit continuations correctly.
 *
 */
class PrepareCallableReferencesForInlining(val context: JvmBackendContext) : FileLoweringPass,
    IrElementTransformerVoidWithContext() {
    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        expression.transformChildrenVoid()
        val function = expression.symbol.owner
        if (function.resolveFakeOverrideOrSelf().isInlineFunctionCall(context)) {
            val function = expression.symbol.owner
            function.parameters.forEach { param ->
                if (param.isInlineParameter()) {
                    val arg = expression.arguments[param]
                    if (arg is IrRichPropertyReference) {
                        expression.arguments[param] = arg.convertToRichFunctionReference(context)
                    }
                    (expression.arguments[param] as? IrRichFunctionReference)?.also {
                        it.origin = IrStatementOrigin.INLINE_LAMBDA
                        it.invokeFunction.origin = IrDeclarationOrigin.INLINE_LAMBDA
                        // Rename <this> -> $receiver
                        it.invokeFunction.parameters.singleOrNull { param -> param.name == SpecialNames.THIS }?.also { param ->
                            param.name = Name.identifier(RECEIVER_PARAMETER_NAME)
                        }
                    }
                }
            }
        }
        return expression
    }

    override fun visitValueParameterNew(declaration: IrValueParameter): IrStatement {
        if (declaration.isInlineSuspendParameter()) {
            val defaultValue = declaration.defaultValue?.expression
            if (defaultValue is IrRichFunctionReference) {
                defaultValue.origin = JvmLoweredStatementOrigin.INLINE_SUSPEND_PARAM_DEFAULT_VALUE
            }
        }
        return super.visitValueParameterNew(declaration)
    }
}

/**
 * Converts
 *
 * RICH_PROPERTY_REFERENCE
 *   bound values
 *   getter: ...
 *     VALUE_PARAMETER <this>
 *     <getter_body>
 *   setter ...
 *
 * ->
 *
 * RICH_FUNCTION_REFERENCE
 *   bound values
 *   invoke:
 *     VALUE_PARAMETER $receiver
 *     <getter_body>
 */
private fun IrRichPropertyReference.convertToRichFunctionReference(context: LoweringContext): IrRichFunctionReference {
    val overriddenClass = context.irBuiltIns.functionN(getterFunction.parameters.size)
    val builder = context.createIrBuilder(getterFunction.symbol).at(this)
    val referenceType = overriddenClass.typeWith(getterFunction.parameters.map { it.type } + getterFunction.returnType)
    return builder.irRichFunctionReference(
        superType = referenceType,
        reflectionTargetSymbol = null,
        overriddenFunctionSymbol = overriddenClass.functions.single { it.name == OperatorNameConventions.INVOKE }.symbol,
        invokeFunction = getterFunction,
        captures = boundValues,
        origin = IrStatementOrigin.LAMBDA,
    ).also {
        it.attributeOwnerId = attributeOwnerId
    }
}