/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirMemberFunctionSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirTopLevelFunctionSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createSignature
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.util.kotlinFunctionInvokeCallableIds
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.WrongSymbolForSamConstructor
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirModuleResolveState
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.getHasStableParameterNames
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal class KtFirFunctionSymbol(
    override val firSymbol: FirNamedFunctionSymbol,
    override val resolveState: LLFirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtFunctionSymbol(), KtFirSymbol<FirNamedFunctionSymbol> {
    override val psi: PsiElement? by cached { firSymbol.findPsi() }
    override val name: Name get() = withValidityAssertion { firSymbol.name }

    override val isBuiltinFunctionInvoke: Boolean
        get() = withValidityAssertion { callableIdIfNonLocal in kotlinFunctionInvokeCallableIds }

    override val returnType: KtType get() = withValidityAssertion { firSymbol.returnType(builder) }
    override val receiverType: KtType? get() = withValidityAssertion { firSymbol.receiverType(builder) }
    
    override val typeParameters by cached { firSymbol.createKtTypeParameters(builder) }
    override val valueParameters: List<KtValueParameterSymbol> by cached { firSymbol.createKtValueParameters(builder) }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { firSymbol.fir.getHasStableParameterNames(firSymbol.moduleData.session) }

    override val annotationsList by cached { KtFirAnnotationListForDeclaration.create(firSymbol, resolveState.rootModuleSession, token) }

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
                firSymbol.containingClass()?.classId == null -> KtSymbolKind.TOP_LEVEL
                else -> KtSymbolKind.CLASS_MEMBER
            }
        }

    override val modality: Modality get() = withValidityAssertion { firSymbol.modalityOrFinal }
    override val visibility: Visibility get() = withValidityAssertion { firSymbol.visibility }

    override fun createPointer(): KtSymbolPointer<KtFunctionSymbol> {
        if (firSymbol.fir.origin != FirDeclarationOrigin.SubstitutionOverride) {
            KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        }

        return when (symbolKind) {
            KtSymbolKind.TOP_LEVEL ->
                KtFirTopLevelFunctionSymbolPointer(firSymbol.callableId, firSymbol.createSignature())

            KtSymbolKind.CLASS_MEMBER ->
                KtFirMemberFunctionSymbolPointer(
                    firSymbol.containingClass()?.classId ?: error("ClassId should not be null for member function"),
                    firSymbol.name,
                    firSymbol.createSignature()
                )

            KtSymbolKind.ACCESSOR -> TODO("Creating symbol for accessors fun is not supported yet")
            KtSymbolKind.LOCAL -> throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(
                callableIdIfNonLocal?.toString() ?: name.asString()
            )
            KtSymbolKind.SAM_CONSTRUCTOR -> throw WrongSymbolForSamConstructor(this::class.java.simpleName)
        }
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
