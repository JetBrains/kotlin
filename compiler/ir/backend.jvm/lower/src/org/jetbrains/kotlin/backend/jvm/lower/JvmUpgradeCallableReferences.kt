/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.UpgradeCallableReferences
import org.jetbrains.kotlin.backend.common.lower.declarationsAtFunctionReferenceLowering
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.suspendFunctionOriginal
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
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
    generateFakeAccessorsForReflectionProperty = true,
) {

    // this shouldn't be needed after moving the lowering outside of per-file part
    override fun selectSAMOverriddenFunction(irClass: IrClass): IrSimpleFunction {
        irClass.declarationsAtFunctionReferenceLowering?.filterIsInstance<IrSimpleFunction>()?.singleOrNull { it.modality == Modality.ABSTRACT }?.let { return it }
        return super.selectSAMOverriddenFunction(irClass).suspendFunctionOriginal()
    }

    // This copying shouldn't be needed after moving this lowering before InventNamesForLocalClasses
    override fun copyNecessaryAttributes(oldReference: IrFunctionExpression, newReference: IrRichFunctionReference) {
        newReference.copyAttributes(oldReference)
    }

    override fun copyNecessaryAttributes(oldReference: IrFunctionReference, newReference: IrRichFunctionReference) {
        newReference.copyAttributes(oldReference)
    }

    override fun copyNecessaryAttributes(oldReference: IrLocalDelegatedPropertyReference, newReference: IrRichPropertyReference) {
        newReference.copyAttributes(oldReference)
    }

    override fun copyNecessaryAttributes(oldReference: IrPropertyReference, newReference: IrRichPropertyReference) {
        newReference.copyAttributes(oldReference)
    }

    override fun IrDeclaration.hasMissingObjectDispatchReceiver(): Boolean = isJvmStaticInObject()
}