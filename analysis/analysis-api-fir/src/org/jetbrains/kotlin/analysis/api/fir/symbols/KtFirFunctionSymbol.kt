/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.base.KtContextReceiver
import org.jetbrains.kotlin.analysis.api.contracts.description.KtContractEffectDeclaration
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.contracts.coneEffectDeclarationToAnalysisApi
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.FirCallableSignature
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirMemberFunctionSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirTopLevelFunctionSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.requireOwnerPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.util.kotlinFunctionInvokeCallableIds
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.UnsupportedSymbolKind
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal class KtFirFunctionSymbol(
    override val firSymbol: FirNamedFunctionSymbol,
    override val analysisSession: KtFirAnalysisSession,
) : KtFunctionSymbol(), KtFirSymbol<FirNamedFunctionSymbol> {
    override val psi: PsiElement? by cached { firSymbol.findPsi() }
    override val name: Name get() = withValidityAssertion { firSymbol.name }

    override val isBuiltinFunctionInvoke: Boolean
        get() = withValidityAssertion { callableIdIfNonLocal in kotlinFunctionInvokeCallableIds }

    override val contractEffects: List<KtContractEffectDeclaration> by cached {
        firSymbol.resolvedContractDescription?.effects
            ?.map(FirEffectDeclaration::effect)
            ?.map { it.coneEffectDeclarationToAnalysisApi(builder, this) }
            .orEmpty()
    }

    override val returnType: KtType get() = withValidityAssertion { firSymbol.returnType(builder) }
    override val receiverParameter: KtReceiverParameterSymbol? get() = withValidityAssertion { firSymbol.receiver(builder) }
    override val contextReceivers: List<KtContextReceiver> by cached { firSymbol.createContextReceivers(builder) }

    override val typeParameters by cached { firSymbol.createKtTypeParameters(builder) }
    override val valueParameters: List<KtValueParameterSymbol> by cached { firSymbol.createKtValueParameters(builder) }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { firSymbol.fir.hasStableParameterNames }

    override val annotationsList by cached {
        KtFirAnnotationListForDeclaration.create(
            firSymbol,
            analysisSession.useSiteSession,
            token,
        )
    }

    override val isSuspend: Boolean get() = withValidityAssertion { firSymbol.isSuspend }
    override val isOverride: Boolean get() = withValidityAssertion { firSymbol.isOverride }
    override val isInfix: Boolean get() = withValidityAssertion { firSymbol.isInfix }
    override val isStatic: Boolean get() = withValidityAssertion { firSymbol.isStatic }


    override val isOperator: Boolean get() = withValidityAssertion { firSymbol.isOperator }
    override val isExternal: Boolean get() = withValidityAssertion { firSymbol.isExternal }
    override val isInline: Boolean get() = withValidityAssertion { firSymbol.isInline }
    override val isExtension: Boolean get() = withValidityAssertion { firSymbol.isExtension }
    override val callableIdIfNonLocal: CallableId? get() = withValidityAssertion { firSymbol.getCallableIdIfNonLocal() }

    override val symbolKind: KtSymbolKind
        get() = withValidityAssertion {
            when {
                firSymbol.isLocal -> KtSymbolKind.LOCAL
                firSymbol.containingClassLookupTag()?.classId == null -> KtSymbolKind.TOP_LEVEL
                else -> KtSymbolKind.CLASS_MEMBER
            }
        }

    override val modality: Modality get() = withValidityAssertion { firSymbol.modalityOrFinal }
    override val visibility: Visibility get() = withValidityAssertion { firSymbol.visibility }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtFunctionSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtFunctionSymbol>(this)?.let { return it }

        return when (val kind = symbolKind) {
            KtSymbolKind.TOP_LEVEL -> KtFirTopLevelFunctionSymbolPointer(
                firSymbol.callableId,
                FirCallableSignature.createSignature(firSymbol),
            )

            KtSymbolKind.CLASS_MEMBER -> KtFirMemberFunctionSymbolPointer(
                requireOwnerPointer(),
                firSymbol.name,
                FirCallableSignature.createSignature(firSymbol),
                isStatic = firSymbol.isStatic,
            )

            KtSymbolKind.LOCAL -> throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(
                callableIdIfNonLocal?.toString() ?: name.asString()
            )

            else -> throw UnsupportedSymbolKind(this::class, kind)
        }
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
