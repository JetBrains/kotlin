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
import org.jetbrains.kotlin.backend.jvm.ir.isInlineOnly
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.load.java.JvmAbi

internal val fakeInliningLocalVariablesLowering = makeIrFilePhase(
    ::FakeInliningLocalVariablesLowering,
    name = "FakeInliningLocalVariablesLowering",
    description = "Add fake locals to identify the range of inlined functions and lambdas"
)

internal class FakeInliningLocalVariablesLowering(val context: JvmBackendContext) : IrInlineReferenceLocator(context), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
    }

    override fun visitFunction(declaration: IrFunction, data: IrDeclaration?) {
        super.visitFunction(declaration, data)
        if (declaration.isInline && !declaration.origin.isSynthetic && declaration.body != null && !declaration.isInlineOnly()) {
            val currentFunctionName = context.methodSignatureMapper.mapFunctionName(declaration)
            val localName = "${JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION}$currentFunctionName"
            declaration.addFakeLocalVariable(localName)
        }
    }

    override fun visitInlineLambda(argument: IrFunctionReference, callee: IrFunction, parameter: IrValueParameter, scope: IrDeclaration) {
        val lambda = argument.symbol.owner
        val argumentToFunctionName = context.methodSignatureMapper.mapFunctionName(callee)
        val lambdaReferenceName = context.getLocalClassType(argument)!!.internalName.substringAfterLast("/")
        val localName = "${JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT}-$argumentToFunctionName-$lambdaReferenceName"
        lambda.addFakeLocalVariable(localName)
    }

    private fun IrFunction.addFakeLocalVariable(name: String) {
        body = context.createIrBuilder(symbol).irBlockBody {
            // Create temporary variable, but make sure it's origin is `DEFINED` so that
            // it will materialize in the code.
            // Also, do not forget to remove $$forInline suffix, otherwise, IDE will not be able to navigate to inline function.
            createTmpVariable(irInt(0), name.removeSuffix(FOR_INLINE_SUFFIX), origin = IrDeclarationOrigin.DEFINED)
            when (val oldBody = body) {
                is IrExpressionBody -> +irReturn(oldBody.expression)
                is IrBlockBody -> oldBody.statements.forEach { +it }
                else -> throw AssertionError("Unexpected body:\n${this@addFakeLocalVariable.dump()}")
            }
        }
    }
}
