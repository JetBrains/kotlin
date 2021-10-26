/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.references

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.references.base.CliKtFe10Reference
import org.jetbrains.kotlin.analysis.api.descriptors.references.base.KtFe10Reference
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.references.KtDestructuringDeclarationReference
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.resolve.BindingContext

abstract class KtFe10DestructuringDeclarationEntry(
    element: KtDestructuringDeclarationEntry
) : KtDestructuringDeclarationReference(element), KtFe10Reference {
    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        check(this is KtFe10AnalysisSession)

        val bindingContext = analyze(element, AnalysisMode.PARTIAL)
        val descriptor = bindingContext[BindingContext.COMPONENT_RESOLVED_CALL, element]?.resultingDescriptor
        return listOfNotNull(descriptor?.toKtCallableSymbol(this))
    }
}

internal class CliKtFe10DestructuringDeclarationEntry(
    element: KtDestructuringDeclarationEntry
) : KtFe10DestructuringDeclarationEntry(element), CliKtFe10Reference {
    override fun canRename(): Boolean {
        return false
    }
}