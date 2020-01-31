/*
 * Copyright 2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.IrInlineReferenceLocator
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.load.java.JvmAbi

internal val fakeInliningLocalVariablesLowering = makeIrFilePhase(
    ::FakeInliningLocalVariablesLowering,
    name = "FakeInliningLocalVariablesLowering",
    description = "Add fake locals to identify the range of inlined functions and lambdas"
)

internal class FakeInliningLocalVariablesLowering(val context: JvmBackendContext) : IrInlineReferenceLocator(context), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(this)
    }

    override fun visitFunctionNew(declaration: IrFunction) {
        declaration.acceptChildrenVoid(this)
        if (declaration.isInline && !declaration.origin.isSynthetic && declaration.body != null) {
            declaration.addFakeInliningLocalVariables()
        }
    }

    override fun visitInlineLambda(argument: IrFunctionReference, callee: IrFunction, parameter: IrValueParameter, scope: IrDeclaration) {
        val lambda = argument.symbol.owner
        if (lambda.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) {
            val argumentToFunctionName = context.methodSignatureMapper.mapFunctionName(callee)
            val lambdaReferenceName = context.getLocalClassType(argument)!!.internalName.substringAfterLast("/")
            val localName = "${JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT}-$argumentToFunctionName-$lambdaReferenceName"
            lambda.addFakeLocalVariable(localName)
        }
    }

    private fun IrFunction.addFakeInliningLocalVariables() {
        val currentFunctionName = context.methodSignatureMapper.mapFunctionName(this)
        val localName = "${JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION}$currentFunctionName"
        addFakeLocalVariable(localName)
    }

    private fun IrFunction.addFakeLocalVariable(name: String) {
        context.createIrBuilder(symbol).run {
            body = irBlockBody {
                // Create temporary variable, but make sure it's origin is `DEFINED` so that
                // it will materialize in the code.
                createTmpVariable(irInt(0), name, origin = IrDeclarationOrigin.DEFINED)
                if (body is IrExpressionBody) {
                    +irReturn((body as IrExpressionBody).expression)
                } else {
                    (body as IrBlockBody).statements.forEach { +it }
                }
            }
        }
    }
}
