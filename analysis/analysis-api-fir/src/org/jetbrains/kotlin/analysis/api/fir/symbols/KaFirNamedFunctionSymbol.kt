/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractEffectDeclaration
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.contracts.coneEffectDeclarationToAnalysisApi
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirMemberFunctionSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirTopLevelFunctionSymbolPointer
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaCannotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaUnsupportedSymbolLocation
import org.jetbrains.kotlin.analysis.api.impl.base.util.kotlinFunctionInvokeCallableIds
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.FirCallableSignature
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal class KaFirNamedFunctionSymbol(
    override val firSymbol: FirNamedFunctionSymbol,
    override val analysisSession: KaFirSession,
) : KaNamedFunctionSymbol(), KaFirSymbol<FirNamedFunctionSymbol> {
    override val psi: PsiElement? get() = withValidityAssertion { firSymbol.findPsi() }
    override val name: Name get() = withValidityAssertion { firSymbol.name }

    override val isBuiltinFunctionInvoke: Boolean
        get() = withValidityAssertion { callableId in kotlinFunctionInvokeCallableIds }

    override val contractEffects: List<KaContractEffectDeclaration>
        get() = withValidityAssertion {
            firSymbol.resolvedContractDescription?.effects
                ?.map(FirEffectDeclaration::effect)
                ?.map { it.coneEffectDeclarationToAnalysisApi(builder, this) }
                .orEmpty()
        }

    override val returnType: KaType get() = withValidityAssertion { firSymbol.returnType(builder) }
    override val receiverParameter: KaReceiverParameterSymbol? get() = withValidityAssertion { firSymbol.receiver(builder) }

    override val contextReceivers: List<KaContextReceiver> get() = withValidityAssertion { firSymbol.createContextReceivers(builder) }

    override val typeParameters: List<KaTypeParameterSymbol> get() = withValidityAssertion { firSymbol.createKtTypeParameters(builder) }
    override val valueParameters: List<KaValueParameterSymbol> get() = withValidityAssertion { firSymbol.createKtValueParameters(builder) }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { firSymbol.fir.hasStableParameterNames }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
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

    override val modality: KaSymbolModality get() = withValidityAssertion { firSymbol.kaSymbolModality }
    override val compilerVisibility: Visibility get() = withValidityAssertion { firSymbol.visibility }

    override fun createPointer(): KaSymbolPointer<KaNamedFunctionSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaNamedFunctionSymbol>(this)?.let { return it }

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

            KaSymbolLocation.LOCAL -> throw KaCannotCreateSymbolPointerForLocalLibraryDeclarationException(
                callableId?.toString() ?: name.asString()
            )

            else -> throw KaUnsupportedSymbolLocation(this::class, kind)
        }
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
