/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.load.java.JvmAbi

// This phase is needed to support correct generation of synthetic `$delegate` methods for optimized delegated properties, in the case
// when receiver of the optimized property reference is a field from an outer class.
//
// Since PropertyReferenceDelegationLowering runs before LocalDeclarationsLowering, fields for captured this (aka `this$0`) are not
// generated yet. And there's no other way to obtain the instance of the outer class on an arbitrary value of an inner class.
// However, we need `$delegate` methods to be static to be non-overridable (and public to be visible in reflection and external tools).
//
// So PropertyReferenceDelegationLowering generates `$delegate` methods for optimized property references as instance methods,
// and this phase, which runs _after_ LocalDeclarationsLowering, transforms them to static methods.
@PhaseDescription(
    name = "MakePropertyDelegateMethodsStatic",
    description = "Make `\$delegate` methods for optimized delegated properties static",
    prerequisite = [PropertyReferenceDelegationLowering::class, JvmLocalDeclarationsLowering::class]
)
internal class MakePropertyDelegateMethodsStaticLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transform(this, null)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        if (!declaration.isSyntheticDelegateMethod()) return super.visitSimpleFunction(declaration)

        val oldParameter = declaration.dispatchReceiverParameter ?: return super.visitSimpleFunction(declaration)
        val newParameter = oldParameter.copyTo(declaration, index = 0)

        return declaration.apply {
            valueParameters =
                listOf(newParameter) + valueParameters.map { it.copyTo(this, index = it.index + 1) }
            dispatchReceiverParameter = null
            body = body?.transform(VariableRemapper(mapOf(oldParameter to newParameter)), null)
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        // There should be no calls to synthetic `$delegate` methods because they aren't accessible in the source code, and we don't
        // generate any calls in the IR. Otherwise we would need to remap arguments in those calls.
        if (expression.symbol.owner.isSyntheticDelegateMethod()) {
            error(
                "`\$delegate` method should not be called. Please either remove the call, or support remapping of dispatch receiver " +
                        "in MakePropertyDelegateMethodsStaticLowering: ${expression.symbol.owner.render()}"
            )
        }
        return super.visitCall(expression)
    }

    private fun IrSimpleFunction.isSyntheticDelegateMethod(): Boolean =
        origin == IrDeclarationOrigin.PROPERTY_DELEGATE && name.asString().endsWith(JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX)
}
