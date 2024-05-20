/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirValueParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.varargElementType
import org.jetbrains.kotlin.name.Name

internal class KaFirValueParameterSymbol(
    override val firSymbol: FirValueParameterSymbol,
    override val analysisSession: KaFirSession,
) : KaValueParameterSymbol(), KaFirSymbol<FirValueParameterSymbol> {
    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override val name: Name get() = withValidityAssertion { firSymbol.name }

    override val isVararg: Boolean get() = withValidityAssertion { firSymbol.isVararg }

    override val isImplicitLambdaParameter: Boolean
        get() = withValidityAssertion {
            firSymbol.source?.kind == KtFakeSourceElementKind.ItLambdaParameter
        }

    override val isCrossinline: Boolean get() = withValidityAssertion { firSymbol.isCrossinline }

    override val isNoinline: Boolean get() = withValidityAssertion { firSymbol.isNoinline }

    override val returnType by cached {
        val returnType = firSymbol.resolvedReturnType
        if (firSymbol.isVararg) {
            builder.typeBuilder.buildKtType(returnType.varargElementType())
        } else {
            builder.typeBuilder.buildKtType(returnType)
        }
    }

    override val hasDefaultValue: Boolean get() = withValidityAssertion { firSymbol.hasDefaultValue }

    override val annotationsList by cached {
        KaFirAnnotationListForDeclaration.create(firSymbol, builder)
    }

    override val generatedPrimaryConstructorProperty: KaKotlinPropertySymbol? by cached {
        val propertySymbol = firSymbol.fir.correspondingProperty?.symbol ?: return@cached null
        val ktPropertySymbol = builder.variableLikeBuilder.buildPropertySymbol(propertySymbol)
        check(ktPropertySymbol is KaKotlinPropertySymbol) {
            "Unexpected symbol for primary constructor property ${ktPropertySymbol.javaClass} for fir: ${firSymbol.fir.renderWithType()}"
        }

        ktPropertySymbol
    }

    override fun createPointer(): KaSymbolPointer<KaValueParameterSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaValueParameterSymbol>(this)?.let { return it }

        val ownerSymbol = with(analysisSession) { getContainingSymbol() }
            ?: error("Containing function is expected for a value parameter symbol")

        requireIsInstance<KaFunctionLikeSymbol>(ownerSymbol)

        return KaFirValueParameterSymbolPointer(
            ownerPointer = analysisSession.createOwnerPointer(this),
            name = name,
            index = (ownerSymbol.firSymbol.fir as FirFunction).valueParameters.indexOf(firSymbol.fir),
        )
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
