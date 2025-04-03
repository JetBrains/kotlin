/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.types.extractTypeParameters
import org.jetbrains.kotlin.ir.util.erasedTopLevelCopy
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.IrVisitor

@Suppress("unused")
class InlineFunctionSerializationPreProcessing(
    private val context: LoweringContext
) : IrVisitor<Unit, IrDeclarationWithVisibility?>(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
    }

    override fun visitElement(element: IrElement, data: IrDeclarationWithVisibility?) {
        element.acceptChildren(this, element as? IrDeclarationWithVisibility ?: data)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: IrDeclarationWithVisibility?) {
        super.visitSimpleFunction(declaration, data)
        if (!declaration.isInline || declaration.body == null || declaration.symbol.isConsideredAsPrivateForInlining()) return
        declaration.erasedTopLevelCopy = declaration.copyAndEraseTypeParameters().convertToTopLevel()
    }

    override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock, data: IrDeclarationWithVisibility?) {
        if (data != null && !data.isEffectivelyPrivate() && inlinedBlock.isEffectivelyPrivate()) {
            inlinedBlock.inlinedFunctionSymbol = null
        }
        super.visitInlinedFunctionBlock(inlinedBlock, data)
    }

    private fun IrSimpleFunction.copyAndEraseTypeParameters(): IrSimpleFunction {
        val typeArguments = extractTypeParameters(this)
            .associate { it.symbol to (if (it.isReified) null else context.irBuiltIns.anyNType) }
        return InlineFunctionBodyPreprocessor(typeArguments, CallInlinerStrategy.DEFAULT)
            .preprocess(this) as IrSimpleFunction
    }

    private fun IrSimpleFunction.convertToTopLevel(): IrSimpleFunction {
        correspondingPropertySymbol = null
        parent = file

        return this
    }

    private fun IrInlinedFunctionBlock.isEffectivelyPrivate(): Boolean {
        return inlinedFunctionSymbol?.isConsideredAsPrivateForInlining() == true
    }
}