/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.serialization.NonLinkingIrInlineFunctionDeserializer
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class EmbedExternalInlinedFunctionLowering(
    private val context: LoweringContext,
    private val inlineFunctionResolver: InlineFunctionResolver,
    irMangler: KotlinMangler.IrMangler,
) : IrVisitorVoid(), ModuleLoweringPass {
    private val externalInlineFunctionDeserializer = NonLinkingIrInlineFunctionDeserializer(
        irBuiltIns = context.irBuiltIns,
        signatureComputer = PublicIdSignatureComputer(irMangler)
    )

    private lateinit var currentFile: IrFile
    private val copiedInlineFunctionsInCurrentFile = mutableListOf<IrSimpleFunction>()
    private val copiedInlineFunctions = mutableMapOf<IrSimpleFunctionSymbol, IrSimpleFunction>()

    override fun lower(irModule: IrModuleFragment) {
        for (file in irModule.files) {
            currentFile = file
            file.acceptVoid(this)
            file.declarations += copiedInlineFunctionsInCurrentFile
            copiedInlineFunctionsInCurrentFile.clear()
        }
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall) {
        var actualCallee = copiedInlineFunctions[expression.symbol]
        if (actualCallee == null) {
            actualCallee = if (expression.symbol.isBound) expression.symbol.owner.erasedTopLevelCopy else null
            if (actualCallee == null) {
                actualCallee = inlineFunctionResolver.getFunctionDeclarationToInline(expression) as IrSimpleFunction?
                    ?: return super.visitCall(expression)
                if (actualCallee.body != null) {
                    // Function from the same module
                    return super.visitCall(expression)
                }
                val body = externalInlineFunctionDeserializer.deserializeInlineFunction(actualCallee)
                    ?: return super.visitCall(expression)

                val functionCopy = actualCallee.deepCopyWithSymbols(currentFile)
                functionCopy.correspondingPropertySymbol = null
                functionCopy.visibility = DescriptorVisibilities.PUBLIC

                copiedInlineFunctionsInCurrentFile += functionCopy
                copiedInlineFunctions[expression.symbol] = functionCopy

                body.acceptChildrenVoid(this)
            }
        }

        expression.symbol = actualCallee.symbol
        super.visitCall(expression)
    }
}