/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compile

import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasBody
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.impl.FirContractCallBlock
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.unwrapSubstitutionOverrides
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * Processes the declaration, collecting files that would need to be submitted to the backend (or handled specially)
 * in case if the declaration is compiled.
 *
 * Besides the file that owns the declaration, the visitor also recursively collects source files with called inline functions.
 * In addition, the visitor collects a list of inlined local classes. Such a list might be useful in [GenerationState.GenerateClassFilter]
 * to filter out class files unrelated to the current compilation.
 *
 * Note that compiled declarations are not analyzed, as the backend can inline them natively.
 */
object CompilationPeerCollector {
    fun process(declaration: FirDeclaration): CompilationPeerData {
        val visitor = CompilationPeerCollectingVisitor()
        visitor.process(declaration)
        return CompilationPeerData(visitor.files, visitor.inlinedClasses)
    }
}

class CompilationPeerData(
    /** File with the original declaration and all files with called inline functions. */
    val files: List<KtFile>,

    /** Local classes inlined as a part of inline functions. */
    val inlinedClasses: Set<KtClassOrObject>
)

private class CompilationPeerCollectingVisitor : FirDefaultVisitorVoid() {
    private val processed = mutableSetOf<FirDeclaration>()
    private val queue = ArrayDeque<FirDeclaration>()

    private val collectedFiles = mutableSetOf<KtFile>()
    private val collectedInlinedClasses = mutableSetOf<KtClassOrObject>()
    private var isInlineFunctionContext = false

    val files: List<KtFile>
        get() = collectedFiles.toList()

    val inlinedClasses: Set<KtClassOrObject>
        get() = collectedInlinedClasses

    fun process(declaration: FirDeclaration) {
        processSingle(declaration)

        while (queue.isNotEmpty()) {
            processSingle(queue.removeFirst())
        }
    }

    private fun processSingle(declaration: FirDeclaration) {
        ProgressManager.checkCanceled()

        if (processed.add(declaration)) {
            val containingFile = declaration.psi?.containingFile
            if (containingFile is KtFile && !containingFile.isCompiled) {
                collectedFiles.add(containingFile)
                declaration.accept(this)
            }
        }
    }

    override fun visitElement(element: FirElement) {
        if (element is FirResolvable) {
            processResolvable(element)
        }

        element.acceptChildren(this)
    }

    override fun visitBlock(block: FirBlock) {
        if (block !is FirContractCallBlock) {
            super.visitBlock(block)
        }
    }

    override fun visitContractDescription(contractDescription: FirContractDescription) {
        // Skip contract description.
        // Contract blocks are skipped in BE, so we would never need to inline contract DSL calls.
    }

    override fun visitConstructor(constructor: FirConstructor) {
        constructor.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

        super.visitConstructor(constructor)
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
        simpleFunction.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

        val oldIsInlineFunctionContext = isInlineFunctionContext
        try {
            isInlineFunctionContext = simpleFunction.isInline
            super.visitFunction(simpleFunction)
        } finally {
            isInlineFunctionContext = oldIsInlineFunctionContext
        }
    }

    override fun visitProperty(property: FirProperty) {
        property.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

        super.visitProperty(property)
    }

    override fun visitClass(klass: FirClass) {
        super.visitClass(klass)

        if (isInlineFunctionContext) {
            collectedInlinedClasses.addIfNotNull(klass.psi as? KtClassOrObject)
        }
    }

    @OptIn(SymbolInternals::class)
    private fun processResolvable(element: FirResolvable) {
        fun addToQueue(function: FirFunction?) {
            val original = function?.unwrapSubstitutionOverrides() ?: return
            if (original.isInline && original.hasBody) {
                queue.add(function)
            }
        }

        val reference = element.calleeReference
        if (reference !is FirResolvedNamedReference) {
            return
        }

        val symbol = reference.resolvedSymbol
        if (symbol is FirCallableSymbol<*>) {
            when (val fir = symbol.fir) {
                is FirFunction -> {
                    addToQueue(fir)
                }
                is FirProperty -> {
                    addToQueue(fir.getter)
                    addToQueue(fir.setter)
                }
                else -> {}
            }
        }
    }
}