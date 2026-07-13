/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.analysis.low.level.api.fir.util.body
import org.jetbrains.kotlin.analysis.low.level.api.fir.backReferences.backReferencedFirFile
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

internal object LLFirPhaseUpdater {
    fun updateDeclarationContent(target: FirElementWithResolveState, newPhase: FirResolvePhase) {
        updatePhaseForNonLocals(target, newPhase, isTargetDeclaration = true)

        if (newPhase == FirResolvePhase.BODY_RESOLVE) {
            val transformer = LocalElementPhaseUpdatingTransformer((target as? FirDeclaration)?.backReferencedFirFile)
            updateDeclarationSignatureBody(target, transformer)

            when (target) {
                is FirVariable -> {
                    target.initializer?.accept(transformer)
                    target.delegate?.accept(transformer)
                    target.getter?.let { updateFunctionBody(it, transformer) }
                    target.setter?.let { updateFunctionBody(it, transformer) }
                    target.backingField?.initializer?.accept(transformer)
                }

                is FirFunction -> updateFunctionBody(target, transformer)
                is FirAnonymousInitializer -> target.body?.accept(transformer)
                is FirCodeFragment -> target.block.accept(transformer)
                is FirDanglingModifierList -> target.acceptChildren(transformer)
            }
        }
    }

    /**
     * Updates the state of the [target] declaration with a partially analyzed body.
     */
    fun updatePartiallyAnalyzedDeclarationContent(target: FirDeclaration, updateSignatureBody: Boolean, statementRange: IntRange) {
        val transformer = LocalElementPhaseUpdatingTransformer(target.backReferencedFirFile)
        if (updateSignatureBody) {
            updateDeclarationSignatureBody(target, transformer)
        }

        if (!statementRange.isEmpty()) {
            val statements = target.body?.statements.orEmpty()
            require(statements.size > statementRange.last)

            val statementsToUpdate = statements.subList(statementRange.first, statementRange.last + 1)
            statementsToUpdate.forEach { it.accept(transformer) }
        }
    }

    private fun updateDeclarationSignatureBody(target: FirElementWithResolveState, transformer: LocalElementPhaseUpdatingTransformer) {
        when (target) {
            is FirConstructor -> {
                target.delegatedConstructor?.accept(transformer)
                updateFunctionSignatureBody(target, transformer)
            }

            is FirFunction -> {
                updateFunctionSignatureBody(target, transformer)
            }

            is FirVariable -> {
                target.getter?.let { updateFunctionSignatureBody(it, transformer) }
                target.setter?.let { updateFunctionSignatureBody(it, transformer) }
            }
        }
    }

    private fun updateFunctionBody(target: FirFunction, transformer: LocalElementPhaseUpdatingTransformer) {
        target.body?.accept(transformer)
    }

    private fun updateFunctionSignatureBody(target: FirFunction, transformer: LocalElementPhaseUpdatingTransformer) {
        target.valueParameters.forEach { it.defaultValue?.accept(transformer) }
    }

    private fun updatePhaseForNonLocals(element: FirElementWithResolveState, newPhase: FirResolvePhase, isTargetDeclaration: Boolean) {
        if (element.resolvePhase >= newPhase) return
        if (!isTargetDeclaration) {
            // phase update for target declaration happens as a declaration publication event after resolve is finished
            @OptIn(ResolveStateAccess::class)
            element.resolveState = newPhase.asResolveState()
        }

        if (element is FirTypeParameterRefsOwner) {
            element.typeParameters.forEach { typeParameter ->
                // if it is not a type parameter of outer declaration
                if (typeParameter is FirTypeParameter) {
                    updatePhaseForNonLocals(typeParameter, newPhase, isTargetDeclaration = false)
                }
            }
        }

        when (element) {
            is FirRegularClass -> {
                element.contextParameters.forEach { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
            }
            is FirScript -> {
                element.receivers.forEach { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
            }
            is FirReplSnippet -> {
                element.receivers.forEach { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
            }
            is FirFunction -> {
                element.valueParameters.forEach { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
                element.receiverParameter?.let { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
                element.contextParameters.forEach { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
            }
            is FirProperty -> {
                element.getter?.let { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
                element.setter?.let { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
                element.backingField?.let { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
                element.receiverParameter?.let { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
                element.contextParameters.forEach { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
            }
            is FirDanglingModifierList -> {
                element.contextParameters.forEach { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
            }
            else -> {}
        }
    }
}

private class LocalElementPhaseUpdatingTransformer(private val firFile: FirFile?) : FirVisitorVoid() {
    override fun visitElement(element: FirElement) {
        if (element is FirElementWithResolveState) {
            @OptIn(ResolveStateAccess::class)
            element.resolveState = FirResolvePhase.BODY_RESOLVE.asResolveState()
        }

        // "Back references to FIR" (KT-70517): declarations synthesized during resolution (e.g. the implicit `it` parameter of a lambda or
        // anonymous functions produced by callable references) are not covered by raw FIR building, so we assign their file back reference
        // as we walk the freshly resolved body.
        //
        // CAUTION: This is a quick workaround in response to the problems described above, found in a few failing tests, and might not be
        // the best solution.
        if (firFile != null && element is FirDeclaration) {
            element.backReferencedFirFile = firFile
        }

        element.acceptChildren(this)
    }
}
