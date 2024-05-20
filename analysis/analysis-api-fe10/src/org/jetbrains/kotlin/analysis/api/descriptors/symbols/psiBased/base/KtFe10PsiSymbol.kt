/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base

import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KaFe10AnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KaFe10Symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.psi.KtElement

internal interface KaFe10PsiSymbol<P : KtElement, D : DeclarationDescriptor> : KaFe10Symbol, KaFe10AnnotatedSymbol {
    override val psi: P
    val descriptor: D?

    override val annotationsObject: Annotations
        get() = withValidityAssertion { descriptor?.annotations ?: Annotations.EMPTY }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { psi.kaSymbolOrigin }
}