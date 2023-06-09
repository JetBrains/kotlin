/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.references.KDocReference
import org.jetbrains.kotlin.idea.references.KtFirReference
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName


internal class KtFirKDocReference(element: KDocName) : KDocReference(element), KtFirReference {
    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        val fullFqName = generateSequence(element) { it.parent as? KDocName }.last().getQualifiedNameAsFqName()
        val selectedFqName = element.getQualifiedNameAsFqName()
        return KDocReferenceResolver.resolveKdocFqName(selectedFqName, fullFqName, element)
    }
}