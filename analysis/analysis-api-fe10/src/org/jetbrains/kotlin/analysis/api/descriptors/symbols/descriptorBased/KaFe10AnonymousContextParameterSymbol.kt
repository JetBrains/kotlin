/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.annotations.KaFe10AnnotationList
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.KotlinType

internal class KaFe10AnonymousContextParameterSymbol(
    val type: KotlinType,
    private val analysisContext: Fe10AnalysisContext,
) : KaContextParameterSymbol() {
    override val annotations: KaAnnotationList
        get() = withValidityAssertion { KaFe10AnnotationList.create(type.annotations, analysisContext) }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { Visibilities.Local }

    override val name: Name
        get() = withValidityAssertion { SpecialNames.NO_NAME_PROVIDED }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { KaSymbolOrigin.SOURCE_MEMBER_GENERATED }

    override val psi: PsiElement?
        get() = withValidityAssertion { null }

    override val returnType: KaType
        get() = withValidityAssertion { type.toKtType(analysisContext) }

    override val token: KaLifetimeToken
        get() = analysisContext.token

    override fun createPointer(): KaSymbolPointer<KaContextParameterSymbol> = withValidityAssertion {
        KaFe10NeverRestoringSymbolPointer()
    }
}