/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.getIntConstArgumentOrNull
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.isFacadeClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

internal class ApiVersionIsAtLeastEvaluationLowering(val context: JvmBackendContext) : FileLoweringPass,
    IrElementTransformer<ApiVersionIsAtLeastEvaluationLowering.Data> {

    private val apiVersion = context.state.languageVersionSettings.apiVersion.version

    data class Data(val currentFunction: IrFunction?, val isInsideInlinedBlock: Boolean)

    override fun lower(irFile: IrFile) {
        irFile.accept(this, Data(currentFunction = null, isInsideInlinedBlock = false))
    }

    override fun visitBlock(expression: IrBlock, data: Data): IrExpression {
        return super.visitBlock(
            expression,
            data.copy(isInsideInlinedBlock = data.isInsideInlinedBlock || expression is IrInlinedFunctionBlock)
        )
    }

    override fun visitFunction(declaration: IrFunction, data: Data): IrStatement {
        return super.visitFunction(declaration, data.copy(currentFunction = declaration))
    }

    override fun visitCall(expression: IrCall, data: Data): IrExpression {
        if (!data.isInsideInlinedBlock
            || !expression.symbol.owner.isApiVersionIsAtLeast
            || isInInlineFunInKotlinRuntime(data.currentFunction)
        ) {
            return super.visitCall(expression, data) as IrExpression
        }

        val epic = expression.getIntConstArgumentOrNull(0) ?: return super.visitCall(expression, data) as IrExpression
        val major = expression.getIntConstArgumentOrNull(1) ?: return super.visitCall(expression, data) as IrExpression
        val minor = expression.getIntConstArgumentOrNull(2) ?: return super.visitCall(expression, data) as IrExpression

        val currentFunction = data.currentFunction
        require(currentFunction != null)
        val builder = context.createJvmIrBuilder(currentFunction.symbol)
        val versionArgument = MavenComparableVersion("$epic.$major.$minor")
        return if (apiVersion >= versionArgument) builder.irTrue() else builder.irFalse()
    }

    private fun isInInlineFunInKotlinRuntime(currentFunction: IrFunction?): Boolean {
        return currentFunction != null && currentFunction.isInline
                && currentFunction.getPackageFragment().packageFqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)
    }

    private val IrFunction.isApiVersionIsAtLeast: Boolean
        get() {
            return name.asString() == "apiVersionIsAtLeast"
                    && getPackageFragment().packageFqName == StandardNames.KOTLIN_INTERNAL_FQ_NAME
                    && parent.isFacadeClass
                    && valueParameters.size == 3
                    && valueParameters[0].type.isInt()
                    && valueParameters[1].type.isInt()
                    && valueParameters[2].type.isInt()
                    && dispatchReceiverParameter == null
                    && extensionReceiverParameter == null
        }
}