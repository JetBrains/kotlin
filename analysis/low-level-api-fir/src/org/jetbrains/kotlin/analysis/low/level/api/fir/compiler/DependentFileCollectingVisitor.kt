/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalMember
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasBody
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.impl.FirContractCallBlock
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.unwrapSubstitutionOverrides
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * Collects files needed for compilation of a given declaration.
 * Basically, it collects files with called inline functions, and a context file if local functions/classes are used.
 */
internal class DependentFileCollectingVisitor(private val session: FirSession) : FirDefaultVisitorVoid() {
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

    private fun recordFile(declaration: FirDeclaration): Boolean {
        val containingFile = declaration.psi?.containingFile
        if (containingFile is KtFile && !containingFile.isCompiled) {
            collectedFiles.add(containingFile)

            // Even if the file is already in the list,
            // there might be different declarations that depend on other files.
            return true
        }

        return false
    }

    private fun processSingle(declaration: FirDeclaration) {
        if (processed.add(declaration) && recordFile(declaration)) {
            declaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            declaration.accept(this)
        }
    }

    override fun visitElement(element: FirElement) {
        when (element) {
            is FirResolvable -> processResolvable(element)
            is FirTypeRef -> processTypeRef(element)
        }

        element.acceptChildren(this)
    }

    override fun visitBlock(block: FirBlock) {
        if (block !is FirContractCallBlock) {
            super.visitBlock(block)
        }
    }

    override fun visitContractDescription(contractDescription: FirContractDescription) {
        // Skip contract description
    }

    override fun visitFunction(function: FirFunction) {
        val oldIsInlineFunctionContext = isInlineFunctionContext
        try {
            isInlineFunctionContext = function.isInline
            super.visitFunction(function)
        } finally {
            isInlineFunctionContext = oldIsInlineFunctionContext
        }
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
            if (original.hasBody) {
                if (original.isInline) {
                    queue.add(function)
                } else if (original.isLocalMember) {
                    recordFile(original)
                }
            }
        }

        val reference = element.calleeReference
        if (reference !is FirResolvedNamedReference) {
            return
        }

        val symbol = reference.resolvedSymbol
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

    private fun processTypeRef(typeRef: FirTypeRef) {
        val symbol = typeRef.toClassLikeSymbol(session) ?: return
        if (symbol.isLocal) {
            recordFile(symbol.fir)
        }
    }
}