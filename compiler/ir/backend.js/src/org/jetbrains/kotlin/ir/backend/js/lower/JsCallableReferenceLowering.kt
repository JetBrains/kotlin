/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.lower.WebCallableReferenceLowering
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.ir.backend.js.utils.compileSuspendAsJsGenerator
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.inline.FunctionInlining
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.primaryConstructor

@PhasePrerequisites(PropertyReferenceLowering::class, FunctionInlining::class)
class JsCallableReferenceLowering(context: JsIrBackendContext) : WebCallableReferenceLowering(context) {

    private val compileSuspendAsJsGenerator = context.compileSuspendAsJsGenerator

    override fun getConstructorCallOrigin(reference: IrRichFunctionReference) = JsStatementOrigins.CALLABLE_REFERENCE_CREATE

    private val IrRichFunctionReference.shouldAddContinuation: Boolean
        get() = isLambda && invokeFunction.isSuspend && !compileSuspendAsJsGenerator

    override fun getClassOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin {
        return if (reference.isKReference || !reference.isLambda) FUNCTION_REFERENCE_IMPL else LAMBDA_IMPL
    }

    override fun IrBuilderWithScope.generateSuperClassConstructorCall(
        constructor: IrConstructor,
        superClassType: IrType,
        functionReference: IrRichFunctionReference,
    ): IrDelegatingConstructorCall {
        val superConstructor = superClassType.classOrFail.owner.primaryConstructor
            ?: compilationException("Missing primary constructor", superClassType.classOrFail.owner)
        return irDelegatingConstructorCall(superConstructor).apply {
            if (functionReference.shouldAddContinuation) {
                val continuation = constructor.parameters.single { it.origin == IrDeclarationOrigin.CONTINUATION }
                arguments[0] = IrGetValueImpl(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    type = continuation.type,
                    symbol = continuation.symbol,
                    origin = JsStatementOrigins.CALLABLE_REFERENCE_INVOKE,
                )
            }
        }
    }

    override fun getSuperClassType(reference: IrRichFunctionReference): IrType {
        return if (reference.shouldAddContinuation) {
            context.symbols.coroutineImpl.owner.defaultType
        } else {
            context.irBuiltIns.anyType
        }
    }

    override fun getExtraConstructorParameters(constructor: IrConstructor, reference: IrRichFunctionReference): List<IrValueParameter> {
        if (!reference.shouldAddContinuation) return emptyList()
        return listOf(
            buildValueParameter(constructor) {
                val superContinuation = context.symbols.coroutineImpl.owner.primaryConstructor!!.parameters.single()
                name = superContinuation.name
                type = superContinuation.type
                origin = IrDeclarationOrigin.CONTINUATION
                kind = IrParameterKind.Regular
            }
        )
    }
}