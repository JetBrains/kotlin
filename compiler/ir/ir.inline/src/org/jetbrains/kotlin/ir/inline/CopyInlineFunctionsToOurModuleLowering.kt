/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.serialization.NonLinkingIrInlineFunctionDeserializer
import org.jetbrains.kotlin.backend.common.serialization.isDeserializedFromOtherModule
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import kotlin.random.Random

class CopyInlineFunctionsToOurModuleLowering(
    private val context: PreSerializationLoweringContext,
    irMangler: KotlinMangler.IrMangler,
) : IrVisitorVoid(), ModuleLoweringPass {
    private val externalInlineFunctionDeserializer = NonLinkingIrInlineFunctionDeserializer(
        irBuiltIns = context.irBuiltIns,
        signatureComputer = PublicIdSignatureComputer(irMangler),
    )

    private lateinit var currentFile: IrFile
    private lateinit var copiedFunctionNameRng: Random
    private val copiedInlineFunctionsInCurrentFile = mutableListOf<IrSimpleFunction>()
    private val originalToCopiedInlineFunctions = mutableMapOf<IrSimpleFunction, IrSimpleFunction>()

    override fun lower(irModule: IrModuleFragment) {
        for (file in irModule.files) {
            currentFile = file
            copiedFunctionNameRng = Random(file.fileEntry.name.hashCode())
            file.acceptVoid(this)
            file.declarations += copiedInlineFunctionsInCurrentFile
            copiedInlineFunctionsInCurrentFile.clear()
        }
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall) {
        val symbol = expression.symbol
        if (!symbol.isBound) {
            return super.visitCall(expression)
        }
        val originalRealFun = symbol.owner.resolveFakeOverrideOrSelf() as? IrSimpleFunction
            ?: return super.visitCall(expression)

        var targetFunction = originalToCopiedInlineFunctions[originalRealFun]
        if (targetFunction == null) {
            if (!shouldCopyFunction(originalRealFun)) {
                return super.visitCall(expression)
            }

            targetFunction = originalRealFun
            if (targetFunction.body == null) {
                targetFunction = externalInlineFunctionDeserializer.deserializeInlineFunction(targetFunction) as IrSimpleFunction?
                    ?: return super.visitCall(expression)
            }
            if (targetFunction.body == null) {
                return super.visitCall(expression)
            }

            val functionCopy = createInlineFunctionCopy(targetFunction)
            copiedInlineFunctionsInCurrentFile += functionCopy
            originalToCopiedInlineFunctions[originalRealFun] = functionCopy
            targetFunction = functionCopy

            targetFunction.body?.acceptVoid(this)
            targetFunction.parameters.forEach { param ->
                if (expression.arguments[param.indexInParameters] == null) {
                    param.defaultValue?.acceptVoid(this)
                }
            }
        }

        expression.symbol = targetFunction.symbol
        super.visitCall(expression)
    }

    private fun shouldCopyFunction(originalRealFun: IrSimpleFunction): Boolean {
        if (!originalRealFun.isInline || originalRealFun.isInlineCopy) {
            return false
        }
        if (Symbols.isTypeOfIntrinsic(originalRealFun.symbol)
            || originalRealFun.symbol == context.symbols.coroutineContextGetter
            || originalRealFun.name.asString() == "suspendCoroutineUninterceptedOrReturn"
        ) {
            return false
        }
        if (originalRealFun !is IrLazyDeclarationBase && !originalRealFun.isDeserializedFromOtherModule) {
            // Function from the same module
            return false
        }

        return true
    }

    private fun createInlineFunctionCopy(originalFunction: IrSimpleFunction): IrSimpleFunction {
        val originalSignature = originalFunction.symbol.signature!!.nearestPublicSig() as IdSignature.CommonSignature
        val newName = inventNameForCopiedFun(originalSignature.declarationFqName)
        val newSignature = originalSignature.let {
            IdSignature.CommonSignature(it.packageFqName, newName, it.id, it.mask, it.description)
        }

        fun remapSignatureInsideCopiedFun(oldSig: IdSignature?): IdSignature? = when (oldSig) {
            null -> null
            is IdSignature.CommonSignature -> {
                if (oldSig == originalFunction.symbol.signature) newSignature else oldSig
            }
            is IdSignature.CompositeSignature -> IdSignature.CompositeSignature(
                remapSignatureInsideCopiedFun(oldSig.container)!!,
                remapSignatureInsideCopiedFun(oldSig.inner)!!,
            )
            else -> oldSig
        }

        val symbolRemapper = DeepCopySymbolRemapper(signatureRemapper = ::remapSignatureInsideCopiedFun)
        originalFunction.acceptVoid(symbolRemapper)
        val deepCopy = DeepCopyIrTreeWithSymbols(symbolRemapper, DeepCopyTypeRemapper(symbolRemapper))
        val functionCopy = originalFunction.transform(deepCopy, null) as IrSimpleFunction
        functionCopy.patchDeclarationParents(currentFile)

        functionCopy.name = Name.identifier(newName)
        functionCopy.visibility = DescriptorVisibilities.INTERNAL
        functionCopy.modality = Modality.FINAL
        functionCopy.correspondingPropertySymbol = null
        functionCopy.annotations = emptyList()
        functionCopy.isInlineCopy = true
        return functionCopy
    }

    private fun inventNameForCopiedFun(oldName: String): String {
        val copyPrefix = "copy-of-inline-"
        val originalName: String
        val id = copiedFunctionNameRng.nextInt(1, Int.MAX_VALUE)
        if (oldName.startsWith(copyPrefix)) {
            val prefixEnd = oldName.indexOf(':', copyPrefix.length)
            originalName = oldName.substring(prefixEnd)
        } else {
            originalName = oldName.replace('.', '-')
        }
        return "$copyPrefix$id:$originalName"
    }
}

private var IrFunction.isInlineCopy by irFlag(copyByDefault = true)