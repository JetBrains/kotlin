/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideOrFail
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

internal class IdSignatureHashCalculator(private val icHasher: ICHasher) {
    private val idSignatureSources = hashMapOf<IdSignature, IdSignatureSource>()
    private val idSignatureHashes = hashMapOf<IdSignature, ICHash>()

    private val fileAnnotationHashes = hashMapOf<IrFile, ICHash>()
    private val constantHashes = hashMapOf<IrProperty, ICHash>()
    private val inlineFunctionFlatHashes = hashMapOf<IrFunction, ICHash>()

    private data class InlineFunctionDependencies(
        val usedInlineFunctions: LinkedHashSet<IrFunction>,
        val usedConstants: LinkedHashSet<IrProperty>,
    )

    private val inlineFunctionDepends = hashMapOf<IrFunction, InlineFunctionDependencies>()

    private val IrFile.annotationsHash: ICHash
        get() = fileAnnotationHashes.getOrPut(this) {
            icHasher.calculateIrAnnotationContainerHash(this)
        }

    private val IrProperty.constantHash: ICHash
        get() = constantHashes.getOrPut(this) {
            icHasher.calculateIrSymbolHash(symbol)
        }

    private val IrFunction.inlineFunctionFlatHash: ICHash
        get() = inlineFunctionFlatHashes.getOrPut(this) {
            val function = if (isFakeOverride && this is IrSimpleFunction) resolveFakeOverrideOrFail() else this
            val flatHash = icHasher.calculateIrFunctionHash(function)
            ICHash(symbol.calculateSymbolHash().hash.combineWith(flatHash.hash))
        }

    private val IrFunction.inlineDepends: InlineFunctionDependencies
        get() = inlineFunctionDepends.getOrPut(this) {
            val usedInlineFunctions = linkedSetOf<IrFunction>()
            val usedConstants = linkedSetOf<IrProperty>()

            acceptVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitCall(expression: IrCall) {
                    val callee = expression.symbol.owner
                    if (callee.isInline) {
                        usedInlineFunctions += callee
                    }
                    val correspondingProperty = callee.correspondingPropertySymbol?.owner
                    if (correspondingProperty?.isConst == true) {
                        usedConstants += correspondingProperty
                    }
                    expression.acceptChildrenVoid(this)
                }

                override fun visitFunctionReference(expression: IrFunctionReference) {
                    val reference = expression.symbol.owner
                    if (reference.isInline) {
                        // this if is fine, because fake overrides are not inlined as function reference calls even as inline function args
                        if (!reference.isFakeOverride) {
                            usedInlineFunctions += reference
                        }
                    }
                    expression.acceptChildrenVoid(this)
                }
            })

            InlineFunctionDependencies(usedInlineFunctions, usedConstants)
        }

    private fun IrSymbol.calculateSymbolHash(): ICHash {
        var srcIrFile = signature?.let { sig -> idSignatureSources[sig]?.srcIrFile }
        if (srcIrFile == null) {
            var parentDeclaration = (owner as? IrDeclaration)?.parent
            while (parentDeclaration is IrDeclaration) {
                parentDeclaration = parentDeclaration.parent
            }
            srcIrFile = parentDeclaration as? IrFile
        }

        val fileAnnotationsHash = srcIrFile?.annotationsHash ?: ICHash()
        return ICHash(fileAnnotationsHash.hash.combineWith(icHasher.calculateIrSymbolHash(this).hash))
    }

    private fun IrFunction.calculateInlineFunctionTransitiveHash(): ICHash {
        var transitiveHash = inlineFunctionFlatHash
        val transitiveDepends = hashSetOf(this)
        val newDependsStack = transitiveDepends.toMutableList()

        while (newDependsStack.isNotEmpty()) {
            val (usedInlineFunctions, usedConstants) = newDependsStack.removeLast().inlineDepends
            for (inlineFunction in usedInlineFunctions) {
                if (transitiveDepends.add(inlineFunction)) {
                    newDependsStack += inlineFunction
                    transitiveHash = ICHash(transitiveHash.hash.combineWith(inlineFunction.inlineFunctionFlatHash.hash))
                }
            }
            for (constant in usedConstants) {
                transitiveHash = ICHash(transitiveHash.hash.combineWith(constant.constantHash.hash))
            }
        }

        return transitiveHash
    }

    fun addAllSignatureSymbols(idSignatureToFile: Map<IdSignature, IdSignatureSource>) {
        idSignatureSources += idSignatureToFile
    }

    operator fun get(signature: IdSignature): ICHash? {
        val hash = idSignatureHashes[signature]
        if (hash != null) {
            return hash
        }

        val signatureSymbol = idSignatureSources[signature]?.symbol ?: return null

        val signatureHash = (signatureSymbol.owner as? IrFunction)?.let { function ->
            function.isInline.ifTrue { function.calculateInlineFunctionTransitiveHash() }
        } ?: signatureSymbol.calculateSymbolHash()

        idSignatureHashes[signature] = signatureHash
        return signatureHash
    }

    operator fun contains(signature: IdSignature): Boolean {
        return signature in idSignatureSources
    }
}
