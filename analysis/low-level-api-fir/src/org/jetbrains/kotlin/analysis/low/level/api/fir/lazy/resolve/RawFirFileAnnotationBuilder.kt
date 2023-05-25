/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtElement

internal fun buildFileFirAnnotation(
    session: FirSession,
    baseScopeProvider: FirScopeProvider,
    fileAnnotation: KtAnnotationEntry,
    replacement: RawFirReplacement? = null
): FirAnnotation {

    val replacementApplier = replacement?.Applier()

    val builder = object : RawFirBuilder(session, baseScopeProvider) {
        inner class VisitorWithReplacement : Visitor() {
            override fun convertElement(element: KtElement, original: FirElement?): FirElement? =
                super.convertElement(replacementApplier?.tryReplace(element) ?: element, original)
        }
    }
    builder.context.packageFqName = fileAnnotation.containingKtFile.packageFqName
    val result = builder.VisitorWithReplacement().convertElement(fileAnnotation, null) as FirAnnotation
    replacementApplier?.ensureApplied()
    return result
}
