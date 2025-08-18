/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.types.extractTypeParameters
import org.jetbrains.kotlin.ir.util.erasedTopLevelCopy
import org.jetbrains.kotlin.ir.util.erasedTopLevelInlineFunctions
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.originalOfErasedTopLevelCopy
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class InlineFunctionSerializationPreProcessing(private val context: LoweringContext) : IrVisitorVoid(), ModuleLoweringPass {
    private val preprocessedFunctions = mutableListOf<IrSimpleFunction>()

    override fun lower(irModule: IrModuleFragment) {
        irModule.acceptChildrenVoid(this)
        irModule.erasedTopLevelInlineFunctions = preprocessedFunctions
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        if (!declaration.isInline || declaration.body == null || declaration.symbol.isConsideredAsPrivateForInlining()) return
        val preprocessed = declaration.copyAndEraseTypeParameters().convertToPrivateTopLevel().erasePrivateSymbols()
        declaration.erasedTopLevelCopy = preprocessed
        preprocessed.originalOfErasedTopLevelCopy = declaration
        preprocessedFunctions += preprocessed
    }

    private fun IrSimpleFunction.copyAndEraseTypeParameters(): IrSimpleFunction {
        val typeArguments = extractTypeParameters(this).filter { !it.isReified }.associate { it.symbol to null }
        return InlineFunctionBodyPreprocessor(typeArguments)
            .preprocess(this) as IrSimpleFunction
    }

    private fun IrSimpleFunction.convertToPrivateTopLevel(): IrSimpleFunction {
        visibility = DescriptorVisibilities.PUBLIC
        correspondingPropertySymbol = null
        parent = file

        return this
    }

    private fun IrSimpleFunction.erasePrivateSymbols(): IrSimpleFunction {
        object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock) {
                inlinedBlock.acceptChildrenVoid(this)
                if (inlinedBlock.isEffectivelyPrivate()) {
                    inlinedBlock.inlinedFunctionSymbol = null
                }
            }
        }.visitElement(this)
        return this
    }

    private fun IrInlinedFunctionBlock.isEffectivelyPrivate(): Boolean {
        return inlinedFunctionSymbol?.isConsideredAsPrivateAndNotLocalForInlining() == true
    }
}