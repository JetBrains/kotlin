/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.parameterName
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirValueParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.varargElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtParameter

internal class KaFirValueParameterSymbol private constructor(
    override val backingPsi: KtParameter?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirValueParameterSymbol>,
) : KaValueParameterSymbol(), KaFirKtBasedSymbol<KtParameter, FirValueParameterSymbol> {
    constructor(declaration: KtParameter, session: KaFirSession) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
    )

    constructor(symbol: FirValueParameterSymbol, session: KaFirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? KtParameter,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.parameterName ?: firSymbol.name }

    override val isVararg: Boolean
        get() = withValidityAssertion { backingPsi?.isVarArg ?: firSymbol.isVararg }

    override val isImplicitLambdaParameter: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
                false
            else
                firSymbol.source?.kind == KtFakeSourceElementKind.ItLambdaParameter
        }

    override val isCrossinline: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.CROSSINLINE_KEYWORD) ?: firSymbol.isCrossinline }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS.visibility }

    override val isNoinline: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.NOINLINE_KEYWORD) ?: firSymbol.isNoinline }

    override val returnType: KaType
        get() = withValidityAssertion {
            val returnType = firSymbol.resolvedReturnType
            if (firSymbol.isVararg) {
                builder.typeBuilder.buildKtType(returnType.varargElementType())
            } else {
                builder.typeBuilder.buildKtType(returnType)
            }
        }

    override val hasDefaultValue: Boolean
        get() = withValidityAssertion { backingPsi?.hasDefaultValue() ?: firSymbol.hasDefaultValue }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val generatedPrimaryConstructorProperty: KaKotlinPropertySymbol?
        get() = withValidityAssertion {
            if (backingPsi != null) {
                return if (backingPsi.hasValOrVar()) {
                    KaFirKotlinPropertySymbol.create(backingPsi, analysisSession)
                } else {
                    null
                }
            }

            val propertySymbol = firSymbol.fir.correspondingProperty?.symbol ?: return null
            return KaFirKotlinPropertySymbol.create(propertySymbol, analysisSession)
        }

    override fun createPointer(): KaSymbolPointer<KaValueParameterSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<KaValueParameterSymbol>()?.let { return it }

        val ownerSymbol = with(analysisSession) { containingDeclaration }
            ?: error("Containing function is expected for a value parameter symbol")

        requireIsInstance<KaFunctionSymbol>(ownerSymbol)

        return KaFirValueParameterSymbolPointer(
            ownerPointer = analysisSession.createOwnerPointer(this),
            name = name,
            index = (ownerSymbol.firSymbol.fir as FirFunction).valueParameters.indexOf(firSymbol.fir),
            originalSymbol = this
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true

        if (!lazyFirSymbol.isInitialized() || !firSymbol.isTypeAliasedConstructorParameter) {
            return psiOrSymbolEquals(other)
        }

        if (other !is KaFirValueParameterSymbol) return false

        // TODO remove manual comparison when KT-72929 is fixed
        return typeAliasedConstructorParametersEqual(firSymbol, other.firSymbol)
    }

    override fun hashCode(): Int {
        if (!lazyFirSymbol.isInitialized() || !firSymbol.isTypeAliasedConstructorParameter) {
            return psiOrSymbolHashCode()
        }

        // TODO remove explicit hashing when KT-72929 is fixed
        return firSymbol.hashCodeForTypeAliasedConstructorParameter()
    }
}
