/*
 * Copyright 2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.mapClass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi

internal val fakeInliningLocalVariablesLowering = makeIrFilePhase(
    ::FakeInliningLocalVariablesLowering,
    name = "FakeInliningLocalVariablesLowering",
    description = "Add fake locals to identify the range of inlined functions and lambdas"
)

internal class FakeInliningLocalVariablesLowering(val context: JvmBackendContext) : IrElementVisitorVoid, FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.acceptChildrenVoid(this)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall) {
        expression.acceptChildrenVoid(this)
        val callee = expression.symbol.owner
        if (callee.isInline) {
            for (i in 0 until expression.valueArgumentsCount) {
                val argument = expression.getValueArgument(i)
                if ((argument is IrBlock) && argument.origin == IrStatementOrigin.LAMBDA) {
                    val lastStatement = argument.statements.last()
                    if (lastStatement is IrFunctionReference) {
                        val localFunForLambda = lastStatement.symbol.owner
                        if (localFunForLambda.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) {
                            localFunForLambda.addFakeInliningLocalVariablesForArguments(callee)
                        }
                    }
                }
            }
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        declaration.acceptChildrenVoid(this)
        if (declaration.isInline && !declaration.origin.isSynthetic && declaration.body != null) {
            declaration.addFakeInliningLocalVariables()
        }
    }

    private fun IrFunction.addFakeInliningLocalVariables() {
        val currentFunctionName = context.methodSignatureMapper.mapFunctionName(this)
        val localName = "${JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION}$currentFunctionName"
        addFakeLocalVariable(localName)
    }

    private fun IrFunction.addFakeInliningLocalVariablesForArguments(callee: IrFunction) {
        val currentFunctionName = context.methodSignatureMapper.mapFunctionName(this)
        val argumentToFunctionName = context.methodSignatureMapper.mapFunctionName(callee)
        val internalName = context.typeMapper.mapClass(parentAsClass).internalName
        val thisType = internalName.substringAfterLast('/', internalName)
        val lambdaReference = "$thisType\$$currentFunctionName"
        val localName = "${JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT}-$argumentToFunctionName-$lambdaReference"
        addFakeLocalVariable(localName)
    }

    private fun IrFunction.addFakeLocalVariable(name: String) {
        context.createIrBuilder(symbol).run {
            body = irBlockBody {
                // Create temporary variable, but make sure it's origin is `DEFINED` so that
                // it will materialize in the code.
                createTmpVariable(irInt(0), name, origin = IrDeclarationOrigin.DEFINED)
                if (body is IrExpressionBody) {
                    +(body as IrExpressionBody).expression
                } else {
                    (body as IrBlockBody).statements.forEach { +it }
                }
            }
        }
    }
}
