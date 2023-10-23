/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.createDynamicType

internal class KtFe10DynamicFunctionDescValueParameterSymbol(
    val owner: KtFe10DescFunctionSymbol,
) : KtValueParameterSymbol(), KtFe10Symbol {
    override val analysisContext: Fe10AnalysisContext
        get() = owner.analysisContext

    override val token: KtLifetimeToken
        get() = owner.token

    override val origin: KtSymbolOrigin
        get() = withValidityAssertion { KtSymbolOrigin.JS_DYNAMIC }

    override val psi: PsiElement?
        get() = withValidityAssertion { null }

    override val annotationsList: KtAnnotationsList
        get() = withValidityAssertion { KtEmptyAnnotationsList(token) }

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

    override val returnType: KtType
        get() = withValidityAssertion { createDynamicType(analysisContext.builtIns).toKtType(analysisContext) }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtValueParameterSymbol> = withValidityAssertion {
        Pointer(owner.createPointer())
    }

    override fun equals(other: Any?): Boolean = other is KtFe10DynamicFunctionDescValueParameterSymbol && other.owner == this.owner
    override fun hashCode(): Int = owner.hashCode()


    private class Pointer(val ownerPointer: KtSymbolPointer<KtFunctionSymbol>) : KtSymbolPointer<KtValueParameterSymbol>() {
        @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
        override fun restoreSymbol(analysisSession: KtAnalysisSession): KtValueParameterSymbol? {
            @Suppress("DEPRECATION")
            val owner = ownerPointer.restoreSymbol(analysisSession) as? KtFe10DescFunctionSymbol ?: return null
            return KtFe10DynamicFunctionDescValueParameterSymbol(owner)
        }
    }
}
