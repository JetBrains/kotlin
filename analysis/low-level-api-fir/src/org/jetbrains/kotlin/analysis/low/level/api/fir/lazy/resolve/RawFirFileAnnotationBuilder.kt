/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtElement

internal fun buildFileFirAnnotation(
    firFile: FirFile,
    fileAnnotation: KtAnnotationEntry,
    replacement: RawFirReplacement,
): FirAnnotation {
    val session = firFile.moduleData.session
    val baseScopeProvider = firFile.moduleData.session.kotlinScopeProvider
    val replacementApplier = replacement.Applier()
    val builder = object : PsiRawFirBuilder(session, baseScopeProvider) {
        inner class VisitorWithReplacement : Visitor() {
            override fun convertElement(element: KtElement, original: FirElement?): FirElement? =
                super.convertElement(replacementApplier.tryReplace(element), original)
        }
    }

    builder.context.packageFqName = fileAnnotation.containingKtFile.packageFqName
    val visitor = builder.VisitorWithReplacement()
    val result = builder.withContainerSymbol(firFile.symbol) {
        visitor.convertElement(fileAnnotation, null) as FirAnnotation
    }

    replacementApplier.ensureApplied()
    return result
}
