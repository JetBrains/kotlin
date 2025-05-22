/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.serialization.NonLinkingIrInlineFunctionDeserializer
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.erasedTopLevelCopy
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.ir.util.erasedTopLevelCopy
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

    private lateinit var module: IrModuleFragment
    private lateinit var externalInlinerFunctionsFile: IrFile
    private val mappedInlineFunctions = mutableMapOf<IrFunctionSymbol, IrFunction>()

    override fun lower(irModule: IrModuleFragment) {
        module = irModule
        externalInlinerFunctionsFile = IrFileImpl(
            NaiveSourceBasedFileEntryImpl("External inliner functions file"),
            IrFileSymbolImpl(),
            FqName("External inliner functions file"),
            irModule,
        )
        irModule.files.add(0, externalInlinerFunctionsFile)

        irModule.acceptChildrenVoid(this)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
        var actualCallee = mappedInlineFunctions[expression.symbol]
        if (actualCallee == null) {
            actualCallee = (expression.symbol.owner as? IrSimpleFunction)?.erasedTopLevelCopy

            if (actualCallee == null) {
                actualCallee = inlineFunctionResolver.getFunctionDeclarationToInline(expression)
                    ?: return
                if (actualCallee.body != null) {
                    // Function from the same module
                    return
                }
                val body = externalInlineFunctionDeserializer.deserializeInlineFunction(actualCallee)
                    ?: return

                val functionCopySignature = IdSignature.CompositeSignature(
                    IdSignature.CommonSignature("externalInlineFunctionCopy", "abcd", null, 0L, null),
                    actualCallee.symbol.signature!!
                )
                val functionCopySymbol = when(actualCallee) {
                    is IrSimpleFunction -> IrSimpleFunctionSymbolImpl(signature = functionCopySignature)
                    is IrConstructor -> IrConstructorSymbolImpl(signature = functionCopySignature)
                }

                // todo: use functionCopySymbol
                val functionCopy = actualCallee.deepCopyWithSymbols(externalInlinerFunctionsFile)
                (functionCopy as? IrSimpleFunction)?.correspondingPropertySymbol = null
                functionCopy.parent = externalInlinerFunctionsFile

                externalInlinerFunctionsFile.declarations += functionCopy
                mappedInlineFunctions[expression.symbol] = functionCopy

                body.acceptChildrenVoid(this)
                /*actualCallee.parameters.forEachIndexed { index, param ->
                    if (expression.arguments[index] == null) {
                        // Default values can recursively reference [callee] - transform only needed.
                        param.defaultValue?.acceptVoid(this)
                    }
                }*/
            }
        }

        when (expression) {
            is IrCall -> expression.symbol = actualCallee as IrSimpleFunctionSymbol
            is IrConstructorCall -> expression.symbol = actualCallee as IrConstructorSymbol
            else -> TODO(expression.render())
        }
        expression.acceptChildrenVoid(this)
    }
}