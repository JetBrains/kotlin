/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KaFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.createDynamicType

internal class KaFe10DynamicFunctionDescValueParameterSymbol(
    val owner: KaFe10DescFunctionSymbol,
) : KaValueParameterSymbol(), KaFe10Symbol {
    override val analysisContext: Fe10AnalysisContext
        get() = owner.analysisContext

    override val token: KaLifetimeToken
        get() = owner.token

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { KaSymbolOrigin.JS_DYNAMIC }

    override val psi: PsiElement?
        get() = withValidityAssertion { null }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { KaEmptyAnnotationList(token) }

    override val name: Name
        get() = withValidityAssertion { Name.identifier("args") }

    override val hasDefaultValue: Boolean
        get() = withValidityAssertion { false }

    override val isVararg: Boolean
        get() = withValidityAssertion { true }

    override val isCrossinline: Boolean
        get() = withValidityAssertion { false }

    override val isNoinline: Boolean
        get() = withValidityAssertion { false }

    override val isImplicitLambdaParameter: Boolean
        get() = withValidityAssertion { false }

    override val returnType: KaType
        get() = withValidityAssertion { createDynamicType(analysisContext.builtIns).toKtType(analysisContext) }

    override fun createPointer(): KaSymbolPointer<KaValueParameterSymbol> = withValidityAssertion {
        Pointer(owner.createPointer())
    }

    override fun equals(other: Any?): Boolean = other is KaFe10DynamicFunctionDescValueParameterSymbol && other.owner == this.owner
    override fun hashCode(): Int = owner.hashCode()


    private class Pointer(val ownerPointer: KaSymbolPointer<KaFunctionSymbol>) : KaSymbolPointer<KaValueParameterSymbol>() {
        @KaImplementationDetail
        override fun restoreSymbol(analysisSession: KaSession): KaValueParameterSymbol? {
            val owner = ownerPointer.restoreSymbol(analysisSession) as? KaFe10DescFunctionSymbol ?: return null
            return KaFe10DynamicFunctionDescValueParameterSymbol(owner)
        }
    }
}
