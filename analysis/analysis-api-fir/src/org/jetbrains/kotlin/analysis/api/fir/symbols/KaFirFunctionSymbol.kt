/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractEffectDeclaration
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.contracts.coneEffectDeclarationToAnalysisApi
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.FirCallableSignature
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirMemberFunctionSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirTopLevelFunctionSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.util.kotlinFunctionInvokeCallableIds
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.UnsupportedSymbolLocation
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal class KaFirFunctionSymbol(
    override val firSymbol: FirNamedFunctionSymbol,
    override val analysisSession: KaFirSession,
) : KaFunctionSymbol(), KaFirSymbol<FirNamedFunctionSymbol> {
    override val psi: PsiElement? by cached { firSymbol.findPsi() }
    override val name: Name get() = withValidityAssertion { firSymbol.name }

    override val isBuiltinFunctionInvoke: Boolean
        get() = withValidityAssertion { callableId in kotlinFunctionInvokeCallableIds }

    override val contractEffects: List<KaContractEffectDeclaration> by cached {
        firSymbol.resolvedContractDescription?.effects
            ?.map(FirEffectDeclaration::effect)
            ?.map { it.coneEffectDeclarationToAnalysisApi(builder, this) }
            .orEmpty()
    }

    override val returnType: KaType get() = withValidityAssertion { firSymbol.returnType(builder) }
    override val receiverParameter: KaReceiverParameterSymbol? get() = withValidityAssertion { firSymbol.receiver(builder) }
    override val contextReceivers: List<KaContextReceiver> by cached { firSymbol.createContextReceivers(builder) }

    override val typeParameters by cached { firSymbol.createKtTypeParameters(builder) }
    override val valueParameters: List<KaValueParameterSymbol> by cached { firSymbol.createKtValueParameters(builder) }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { firSymbol.fir.hasStableParameterNames }

    override val annotations by cached {
        KaFirAnnotationListForDeclaration.create(firSymbol, builder)
    }

    override val isSuspend: Boolean get() = withValidityAssertion { firSymbol.isSuspend }
    override val isOverride: Boolean get() = withValidityAssertion { firSymbol.isOverride }
    override val isInfix: Boolean get() = withValidityAssertion { firSymbol.isInfix }
    override val isStatic: Boolean get() = withValidityAssertion { firSymbol.isStatic }
    override val isTailRec: Boolean get() = withValidityAssertion { firSymbol.isTailRec }
    override val isOperator: Boolean get() = withValidityAssertion { firSymbol.isOperator }
    override val isExternal: Boolean get() = withValidityAssertion { firSymbol.isExternal }
    override val isInline: Boolean get() = withValidityAssertion { firSymbol.isInline }
    override val isExtension: Boolean get() = withValidityAssertion { firSymbol.isExtension }
    override val isActual: Boolean get() = withValidityAssertion { firSymbol.isActual }
    override val isExpect: Boolean get() = withValidityAssertion { firSymbol.isExpect }

    override val callableId: CallableId? get() = withValidityAssertion { firSymbol.getCallableId() }

    override val location: KaSymbolLocation
        get() = withValidityAssertion {
            when {
                firSymbol.origin == FirDeclarationOrigin.DynamicScope -> KaSymbolLocation.CLASS
                firSymbol.isLocal -> KaSymbolLocation.LOCAL
                firSymbol.containingClassLookupTag()?.classId == null -> KaSymbolLocation.TOP_LEVEL
                else -> KaSymbolLocation.CLASS
            }
        }

    override val modality: Modality get() = withValidityAssertion { firSymbol.modality }
    override val visibility: Visibility get() = withValidityAssertion { firSymbol.visibility }

    override fun createPointer(): KaSymbolPointer<KaFunctionSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaFunctionSymbol>(this)?.let { return it }

        return when (val kind = location) {
            KaSymbolLocation.TOP_LEVEL -> KaFirTopLevelFunctionSymbolPointer(
                firSymbol.callableId,
                FirCallableSignature.createSignature(firSymbol),
            )

            KaSymbolLocation.CLASS -> KaFirMemberFunctionSymbolPointer(
                analysisSession.createOwnerPointer(this),
                firSymbol.name,
                FirCallableSignature.createSignature(firSymbol),
                isStatic = firSymbol.isStatic,
            )

            KaSymbolLocation.LOCAL -> throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(
                callableId?.toString() ?: name.asString()
            )

            else -> throw UnsupportedSymbolLocation(this::class, kind)
        }
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
