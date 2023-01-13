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
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid


internal class IdSignatureHashCalculator {
    private val flatHashes = hashMapOf<IrFunction, ICHash>()
    private val inlineFunctionCallGraph = hashMapOf<IrFunction, Set<IrFunction>>()
    private val processingFunctions = hashSetOf<IrFunction>()
    private val functionTransitiveHashes = hashMapOf<IrFunction, ICHash>()
    private val fileAnnotationHashes = hashMapOf<IrFile, ICHash>()

    private val allIdSignatureHashes = hashMapOf<IdSignature, ICHash>()

    private val IrFile.annotationsHash: ICHash
        get() = fileAnnotationHashes.getOrPut(this, ::irAnnotationContainerHashForIC)

    private inner class FlatHashCalculator : IrElementVisitorVoid {
        private var fileAnnotationsHash = ICHash()

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitFile(declaration: IrFile, data: Nothing?) {
            fileAnnotationsHash = declaration.annotationsHash
            declaration.acceptChildrenVoid(this)
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction) {
            if (declaration.isInline) {
                if (declaration in flatHashes) {
                    return
                }

                val inlineFunctionFlatHash = if (declaration.isFakeOverride) {
                    declaration.resolveFakeOverride()?.irSimpleFunctionHashForIC()
                        ?: icError("can not resolve fake override for ${declaration.render()}")
                } else {
                    declaration.irSimpleFunctionHashForIC()
                }

                flatHashes[declaration] = fileAnnotationsHash.combineWith(inlineFunctionFlatHash)
            }
            // go deeper since local inline special declarations (like a reference adaptor) may appear
            declaration.acceptChildrenVoid(this)
        }
    }

    private inner class InlineFunctionCallGraphBuilder : IrElementVisitor<Unit, MutableSet<IrFunction>> {
        override fun visitElement(element: IrElement, data: MutableSet<IrFunction>) {
            element.acceptChildren(this, data)
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: MutableSet<IrFunction>) {
            if (declaration in inlineFunctionCallGraph) {
                return
            }
            val newGraph = mutableSetOf<IrFunction>()
            inlineFunctionCallGraph[declaration] = newGraph
            declaration.acceptChildren(this, newGraph)
        }

        override fun visitCall(expression: IrCall, data: MutableSet<IrFunction>) {
            val callee = expression.symbol.owner
            if (callee.isInline) {
                data += callee
            }
            expression.acceptChildren(this, data)
        }

        override fun visitFunctionReference(expression: IrFunctionReference, data: MutableSet<IrFunction>) {
            val reference = expression.symbol.owner
            if (reference.isInline) {
                // this if is fine, because fake overrides are not inlined as function reference calls even as inline function args
                if (!reference.isFakeOverride) {
                    data += reference
                }
            }
            expression.acceptChildren(this, data)
        }
    }

    private fun getInlineFunctionTransitiveHash(f: IrFunction): ICHash = functionTransitiveHashes.getOrPut(f) {
        if (!processingFunctions.add(f)) {
            icError("inline circle through function ${f.render()} detected")
        }
        val callees = inlineFunctionCallGraph[f] ?: icError("inline function is missed in inline graph ${f.render()}")
        val flatHash = flatHashes[f] ?: icError("no flat hash for ${f.render()}")
        var functionInlineHash = flatHash
        for (callee in callees) {
            functionInlineHash = functionInlineHash.combineWith(getInlineFunctionTransitiveHash(callee))
        }
        processingFunctions.remove(f)
        f.symbol.signature?.let { allIdSignatureHashes[it] = functionInlineHash }
        functionInlineHash
    }

    private fun updateTransitiveHashesByCallGraph() {
        for ((f, callees) in inlineFunctionCallGraph.entries) {
            if (f.isInline) {
                getInlineFunctionTransitiveHash(f)
            } else {
                callees.forEach(::getInlineFunctionTransitiveHash)
            }
        }
    }

    fun updateInlineFunctionTransitiveHashes(fragments: Collection<IrModuleFragment>) {
        fragments.forEach { it.acceptVoid(FlatHashCalculator()) }
        fragments.forEach { it.acceptChildren(InlineFunctionCallGraphBuilder(), mutableSetOf()) }
        updateTransitiveHashesByCallGraph()
    }

    fun addAllSignatureSymbols(idSignatureToFile: Map<IdSignature, IdSignatureSource>) {
        for ((signature, signatureSrc) in idSignatureToFile) {
            if (signature !in allIdSignatureHashes) {
                val fileAnnotationsHash = signatureSrc.srcIrFile.annotationsHash
                allIdSignatureHashes[signature] = fileAnnotationsHash.combineWith(signatureSrc.symbol.irSymbolHashForIC())
            }
        }
    }

    operator fun get(signature: IdSignature) = allIdSignatureHashes[signature]
}
