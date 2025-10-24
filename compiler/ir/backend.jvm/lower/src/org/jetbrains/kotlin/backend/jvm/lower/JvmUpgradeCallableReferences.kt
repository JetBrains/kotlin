/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.UpgradeCallableReferences
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.localClassType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrRichPropertyReference
import org.jetbrains.kotlin.ir.visitors.IrTransformer

internal class JvmUpgradeCallableReferences(context: JvmBackendContext) : UpgradeCallableReferences(
    context = context,
    upgradeFunctionReferencesAndLambdas = true,
    upgradePropertyReferences = true,
    upgradeLocalDelegatedPropertyReferences = true,
    upgradeSamConversions = true,
    upgradeExtractedAdaptedBlocks = true,
    castDispatchReceiver = false,
) {
    private val jvmSymbols = context.symbols

    // TODO change after KT-78719
    override fun IrTransformer<IrDeclarationParent>.processCallExpression(expression: IrCall, data: IrDeclarationParent): IrElement {
        val function = expression.symbol.owner
        if (function.symbol == jvmSymbols.indyLambdaMetafactoryIntrinsic) {
            for ((i, element) in expression.arguments.withIndex()) {
                expression.arguments[i] = if (i == 1) {
                    element?.transformChildren(this, data)
                    element
                } else {
                    element?.transform(this, data)
                }
            }
            return expression
        }
        expression.transformChildren(this, data)
        return expression
    }

    override fun copyNecessaryAttributes(oldReference: IrFunctionReference, newReference: IrRichFunctionReference) {
        newReference.localClassType = oldReference.localClassType
    }

    override fun copyNecessaryAttributes(oldReference: IrLocalDelegatedPropertyReference, newReference: IrRichPropertyReference) {
        newReference.localClassType = oldReference.localClassType
    }

    override fun copyNecessaryAttributes(oldReference: IrPropertyReference, newReference: IrRichPropertyReference) {
        newReference.localClassType = oldReference.localClassType
    }

    override fun IrDeclaration.isMissingObjectDispatchReceiver(): Boolean = isJvmStaticInObject()
}
