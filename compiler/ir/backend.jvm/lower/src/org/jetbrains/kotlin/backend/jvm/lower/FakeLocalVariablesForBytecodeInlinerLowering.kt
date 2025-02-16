/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.ir.syntheticBodyIsNotSupported
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.IrInlineReferenceLocator
import org.jetbrains.kotlin.backend.jvm.ir.isInlineOnly
import org.jetbrains.kotlin.backend.jvm.localClassType
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.load.java.JvmAbi

interface FakeInliningLocalVariables<Container : IrElement> {
    val context: JvmBackendContext

    fun Container.addFakeLocalVariable(name: String)

    fun Container.addFakeLocalVariableForFun(declaration: IrDeclaration) {
        if (declaration !is IrFunction) return
        if (declaration.isInline && !declaration.origin.isSynthetic && declaration.body != null && !declaration.isInlineOnly()) {
            val currentFunctionName = context.defaultMethodSignatureMapper.mapFunctionName(declaration)
            val localName = "${JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION}$currentFunctionName"
            this.addFakeLocalVariable(localName)
        }
    }

    fun Container.addFakeLocalVariableForLambda(argument: IrElement, callee: IrFunction) {
        val argumentToFunctionName = context.defaultMethodSignatureMapper.mapFunctionName(callee)
        val lambdaReferenceName = argument.localClassType!!.internalName.substringAfterLast("/")
        val localName = "${JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT}-$argumentToFunctionName-$lambdaReferenceName"
        this.addFakeLocalVariable(localName)
    }
}

/**
 * Adds fake locals to identify the range of inlined functions and lambdas.
 */
@PhaseDescription(name = "FakeLocalVariablesForBytecodeInlinerLowering")
internal class FakeLocalVariablesForBytecodeInlinerLowering(
    override val context: JvmBackendContext
) : IrInlineReferenceLocator(context), FakeInliningLocalVariables<IrFunction>, FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
    }

    override fun visitFunction(declaration: IrFunction, data: IrDeclaration?) {
        super.visitFunction(declaration, data)
        declaration.addFakeLocalVariableForFun(declaration)
    }

    override fun visitInlineLambda(argument: IrFunctionReference, callee: IrFunction, parameter: IrValueParameter, scope: IrDeclaration) {
        val lambda = argument.symbol.owner
        lambda.addFakeLocalVariableForLambda(argument, callee)
    }

    override fun IrFunction.addFakeLocalVariable(name: String) {
        body = context.createIrBuilder(symbol).irBlockBody {
            // Create temporary variable, but make sure it's origin is `DEFINED` so that
            // it will materialize in the code.
            // Also, do not forget to remove $$forInline suffix, otherwise, IDE will not be able to navigate to inline function.
            createTmpVariable(irInt(0), name.removeSuffix(FOR_INLINE_SUFFIX), origin = IrDeclarationOrigin.DEFINED)
            when (val oldBody = body) {
                is IrExpressionBody -> +irReturn(oldBody.expression)
                is IrBlockBody -> oldBody.statements.forEach { +it }
                is IrSyntheticBody -> syntheticBodyIsNotSupported(this@addFakeLocalVariable)
                null -> compilationException("Missing body", this@addFakeLocalVariable)
            }
        }
    }
}
