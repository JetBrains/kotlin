/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.structure

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.DuplicatedFirSourceElementsException
import org.jetbrains.kotlin.idea.fir.low.level.api.util.isErrorElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTypeReference

internal open class FirElementsRecorder : FirVisitor<Unit, MutableMap<KtElement, FirElement>>() {
    private fun cache(psi: KtElement, fir: FirElement, cache: MutableMap<KtElement, FirElement>) {
        val existingFir = cache[psi]
        if (existingFir != null && existingFir !== fir) {
            when {
                existingFir is FirTypeRef && fir is FirTypeRef && psi is KtTypeReference -> {
                    // FirTypeRefs are often created during resolve
                    // a lot of them with have the same source
                    // we want to take the most "resolved one" here
                    if (fir is FirResolvedTypeRefImpl && existingFir !is FirResolvedTypeRefImpl) {
                        cache[psi] = fir
                    }
                }
                existingFir.isErrorElement && !fir.isErrorElement -> {
                    // TODO better handle error elements
                    // but for now just take first non-error one if such exist
                    cache[psi] = fir
                }
                existingFir.isErrorElement || fir.isErrorElement -> {
                    // do nothing and maybe upgrade to a non-error element in the branch above in the future
                }
                else -> {
                    if (DuplicatedFirSourceElementsException.IS_ENABLED) {
                        throw DuplicatedFirSourceElementsException(existingFir, fir, psi)
                    }
                }
            }
        }
        if (existingFir == null) {
            cache[psi] = fir
        }
    }

    override fun visitElement(element: FirElement, data: MutableMap<KtElement, FirElement>) {
        (element.realPsi as? KtElement)?.let { psi ->
            cache(psi, element, data)
        }
        element.acceptChildren(this, data)
    }

    //@formatter:off
    override fun visitReference(reference: FirReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitNamedReference(namedReference: FirNamedReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitDelegateFieldReference(delegateFieldReference: FirDelegateFieldReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitBackingFieldReference(backingFieldReference: FirBackingFieldReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitSuperReference(superReference: FirSuperReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitThisReference(thisReference: FirThisReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: MutableMap<KtElement, FirElement>) {}
    //@formatter:on

    override fun visitUserTypeRef(userTypeRef: FirUserTypeRef, data: MutableMap<KtElement, FirElement>) {
        userTypeRef.acceptChildren(this, data)
    }

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        fun recordElementsFrom(firDeclaration: FirDeclaration, recorder: FirElementsRecorder): Map<KtElement, FirElement> =
            buildMap { firDeclaration.accept(recorder, this) }
    }
}