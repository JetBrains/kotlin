/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createDelegatingCallWithPlaceholderTypeArguments
import org.jetbrains.kotlin.backend.jvm.ir.isSimpleFunctionCompiledToJvmDefault
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.hasInterfaceParent
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Redirects interface calls with default arguments to DefaultImpls (except methods compiled to JVM defaults).
 */
@PhaseDescription(name = "InterfaceDefaultCalls")
internal class InterfaceDefaultCallsLowering(val context: JvmBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {
    // TODO If there are no default _implementations_ we can avoid generating defaultImpls class entirely by moving default arg dispatchers to the interface class
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val callee = expression.symbol.owner

        if (!callee.hasInterfaceParent() ||
            callee.origin != IrDeclarationOrigin.Companion.FUNCTION_FOR_DEFAULT_PARAMETER ||
            callee.isSimpleFunctionCompiledToJvmDefault(context.config.jvmDefaultMode)
        ) {
            return super.visitCall(expression)
        }

        val redirectTarget = context.cachedDeclarations.getDefaultImplsFunction(callee)

        // InterfaceLowering bridges from DefaultImpls in compatibility mode -- if that's the case,
        // this phase will inadvertently cause a recursive loop as the bridge on the DefaultImpls
        // gets redirected to call itself.
        if (redirectTarget == currentFunction?.irElement) return super.visitCall(expression)

        val newCall = createDelegatingCallWithPlaceholderTypeArguments(expression, redirectTarget, context.irBuiltIns)

        return super.visitCall(newCall)
    }
}
