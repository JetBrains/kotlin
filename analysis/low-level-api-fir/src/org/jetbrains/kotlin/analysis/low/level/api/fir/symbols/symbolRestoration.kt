/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.impl.base.util.withPsiEntry
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findCompiledFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findSourceNonLocalFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.nonLocalContainerForFirLookup
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

/**
 * Whether [restoreSymbol] can restore the symbol of a declaration with this [PsiElement] as its source.
 */
internal val PsiElement.isSymbolRestorationSupported: Boolean
    get() = when (this) {
        is KtDeclaration, is KtFile, is KtContextReceiver -> true
        is KtTypeReference -> isReceiverTypeReference || isContextReceiverTypeReference

        // TODO KT-87715: Support `PsiClass` and Java members so that we can restore Java symbols.
        else -> false
    }

/**
 * Restores the symbol of the declaration with the given [psi] element as its source, in exactly this session.
 *
 * The restored symbol is not additionally resolved, but it might be in any [FirResolvePhase].
 *
 * The session must be valid: when a session is invalidated, all objects which might reference its symbols should be invalidated as well,
 * so restoring a symbol in an invalidated session indicates a bug.
 *
 * The [psi] element must be in the content scope of the session's [ktModule]. This is not checked by the function for performance reasons
 * and should be ensured at the call site.
 *
 * In contrast to [LLResolutionFacade.resolveToFirSymbol], [restoreSymbol] has two advantages:
 *
 * - **Specificity:** The restored symbol always belongs to this session. This is ideal for symbol IDs, which are bound to specific
 *   sessions. Without this guarantee, in case of inconsistent state or duplicate sessions, a symbol ID might restore into the wrong session
 *   without any notice.
 * - **Performance:** The function operates on an already known session, so it does not need to retrieve the module first. This avoids
 *   costly module lookup.
 */
internal fun LLFirSession.restoreSymbol(psi: PsiElement): FirBasedSymbol<*> {
    checkWithAttachment(isValid, { "Cannot restore a symbol in an invalidated session." }) {
        withPsiEntry("psi", psi, this@restoreSymbol.ktModule)
        withEntry("session", this@restoreSymbol.toString())
    }

    return when (psi) {
        is KtFile -> resolvableSession(psi).moduleComponents.firFileBuilder.buildRawFirFileWithCaching(psi).symbol
        is KtDeclaration -> restoreDeclarationSymbol(psi)
        is KtContextReceiver -> restoreContextParameterSymbol(psi, psi.ownerDeclaration)
        is KtTypeReference if psi.isReceiverTypeReference -> restoreReceiverParameterSymbol(psi, psi.parent as KtCallableDeclaration)
        is KtTypeReference if psi.isContextReceiverTypeReference ->
            restoreContextParameterSymbol(psi, (psi.parent as KtContextReceiver).ownerDeclaration)
        else -> restorationError("Restoring a symbol is not supported for this PSI element.", psi)
    }
}

private fun LLFirSession.restoreDeclarationSymbol(declaration: KtDeclaration): FirBasedSymbol<*> {
    if (declaration.containingKtFile.isCompiled) {
        return findCompiledFirSymbol(declaration)
    }

    if (declaration.nonLocalContainerForFirLookup == declaration) {
        return resolvableSession(declaration).findSourceNonLocalFirSymbol(declaration)
    }

    // Local declarations require body resolution of their non-local container, which is only available through the resolution facade.
    val symbol = ktModule.getResolutionFacade(ktModule.project).resolveToFirSymbol(declaration, FirResolvePhase.RAW_FIR)
    checkWithAttachment(
        symbol.moduleData.session == this,
        { "The restored symbol of a local declaration belongs to a different session." },
    ) {
        withPsiEntry("declaration", declaration, this@restoreDeclarationSymbol.ktModule)
        withEntry("session", this@restoreDeclarationSymbol.toString())
        withEntry("symbolSession", symbol.moduleData.session.toString())
    }
    return symbol
}

private fun LLFirSession.restoreReceiverParameterSymbol(
    receiverTypeReference: KtTypeReference,
    owner: KtCallableDeclaration,
): FirBasedSymbol<*> {
    val ownerFir = restoreDeclarationSymbol(owner).fir as? FirCallableDeclaration
    return ownerFir?.receiverParameter?.symbol
        ?: restorationError("The owner of a receiver type reference has no receiver parameter.", receiverTypeReference)
}

private fun LLFirSession.restoreContextParameterSymbol(psi: PsiElement, owner: KtDeclaration?): FirBasedSymbol<*> {
    if (owner == null) {
        restorationError("A context receiver without an owner declaration has no symbol.", psi)
    }

    val contextParameters: List<FirValueParameter> = when (val ownerFir = restoreDeclarationSymbol(owner).fir) {
        is FirCallableDeclaration -> ownerFir.contextParameters
        is FirRegularClass -> ownerFir.contextParameters
        else -> emptyList()
    }

    return contextParameters.firstOrNull { it.psi == psi }?.symbol
        ?: restorationError("The context parameter was not found in its owner declaration.", psi)
}

private fun LLFirSession.resolvableSession(psi: PsiElement): LLFirResolvableModuleSession =
    this as? LLFirResolvableModuleSession ?: restorationError("Restoring a source symbol requires a resolvable session.", psi)

private fun LLFirSession.restorationError(message: String, psi: PsiElement): Nothing {
    errorWithAttachment(message) {
        withPsiEntry("psi", psi, this@restorationError.ktModule)
        withEntry("session", this@restorationError.toString())
    }
}

private val KtTypeReference.isReceiverTypeReference: Boolean
    get() = (parent as? KtCallableDeclaration)?.receiverTypeReference == this

private val KtTypeReference.isContextReceiverTypeReference: Boolean
    get() = parent is KtContextReceiver
