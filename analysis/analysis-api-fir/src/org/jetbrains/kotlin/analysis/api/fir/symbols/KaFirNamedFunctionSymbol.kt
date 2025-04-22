/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractEffectDeclaration
import org.jetbrains.kotlin.analysis.api.fir.*
import org.jetbrains.kotlin.analysis.api.fir.contracts.coneEffectDeclarationToAnalysisApi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirDynamicFunctionSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirMemberFunctionSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirTopLevelFunctionSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaCannotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaUnsupportedSymbolLocation
import org.jetbrains.kotlin.analysis.api.impl.base.util.callableId
import org.jetbrains.kotlin.analysis.api.impl.base.util.kotlinFunctionInvokeCallableIds
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
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
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.isExpectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration

internal class KaFirNamedFunctionSymbol private constructor(
    override val backingPsi: KtNamedFunction?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirNamedFunctionSymbol>,
) : KaNamedFunctionSymbol(), KaFirKtBasedSymbol<KtNamedFunction, FirNamedFunctionSymbol> {
    init {
        require(backingPsi?.isAnonymous != true)
    }

    constructor(declaration: KtNamedFunction, session: KaFirSession) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
    )

    constructor(symbol: FirNamedFunctionSymbol, session: KaFirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? KtNamedFunction,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val psi: PsiElement? get() = withValidityAssertion { backingPsi ?: findPsi() }
    override val name: Name get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: firSymbol.name }

    override val isBuiltinFunctionInvoke: Boolean
        get() = withValidityAssertion { callableId in kotlinFunctionInvokeCallableIds }

    override val contractEffects: List<KaContractEffectDeclaration>
        get() = withValidityAssertion {
            if (backingPsi != null && !backingPsi.mayHaveContract() && !backingPsi.hasContractEffectList()) {
                emptyList()
            } else {
                firSymbol.resolvedContractDescription?.effects
                    ?.map(FirEffectDeclaration::effect)
                    ?.map { it.coneEffectDeclarationToAnalysisApi(builder, this) }
                    .orEmpty()
            }
        }

    override val returnType: KaType
        get() = withValidityAssertion { createReturnType() }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion {
            KaFirReceiverParameterSymbol.create(backingPsi, analysisSession, this)
        }

    override val contextReceivers: List<KaContextReceiver>
        get() = withValidityAssertion { createContextReceivers() }

    override val contextParameters: List<KaContextParameterSymbol>
        get() = withValidityAssertion {
            createKaContextParameters() ?: firSymbol.createKaContextParameters(builder)
        }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion {
            createKaTypeParameters() ?: firSymbol.createKtTypeParameters(builder)
        }

    override val valueParameters: List<KaValueParameterSymbol>
        get() = withValidityAssertion {
            createKaValueParameters() ?: firSymbol.createKtValueParameters(builder)
        }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
                true
            else
                firSymbol.fir.hasStableParameterNames
        }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val isSuspend: Boolean
        get() = withValidityAssertion {
            psiHasModifierIfNotInherited(KtTokens.SUSPEND_KEYWORD) ?: firSymbol.isSuspend
        }

    /**
     * Some modifiers can be inherited, so we cannot check them by PSI in this case.
     *
     * Returns not null output of [org.jetbrains.kotlin.psi.KtModifierListOwnerStub.hasModifier]
     * if [backingPsi] is not null and the symbol is not [isOverride].
     */
    private fun psiHasModifierIfNotInherited(modifierToken: KtModifierKeywordToken): Boolean? {
        if (backingPsi == null || isOverride) return null
        return backingPsi.hasModifier(modifierToken)
    }

    override val isOverride: Boolean
        get() = withValidityAssertion {
            // Library PSI elements doesn't have `override` modifier
            ifSource { backingPsi }?.hasModifier(KtTokens.OVERRIDE_KEYWORD) ?: firSymbol.isOverride
        }

    override val isInfix: Boolean
        get() = withValidityAssertion {
            psiHasModifierIfNotInherited(KtTokens.INFIX_KEYWORD) ?: firSymbol.isInfix
        }

    override val isStatic: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
            // Kotlin doesn't have static functions
                false
            else
                firSymbol.isStatic
        }

    override val isTailRec: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.TAILREC_KEYWORD) ?: firSymbol.isTailRec }

    override val isOperator: Boolean
        get() = withValidityAssertion {
            psiHasModifierIfNotInherited(KtTokens.OPERATOR_KEYWORD) ?: firSymbol.isOperator
        }

    override val isExternal: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.EXTERNAL_KEYWORD) ?: firSymbol.isExternal }

    override val isInline: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.INLINE_KEYWORD) ?: firSymbol.isInline }

    override val isExtension: Boolean
        get() = withValidityAssertion { backingPsi?.isExtensionDeclaration() ?: firSymbol.isExtension }

    override val isActual: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.ACTUAL_KEYWORD) ?: firSymbol.isActual }

    override val isExpect: Boolean
        get() = withValidityAssertion { backingPsi?.isExpectDeclaration() ?: firSymbol.isExpect }

    override val callableId: CallableId?
        get() = withValidityAssertion {
            if (backingPsi != null)
                backingPsi.callableId
            else
                firSymbol.getCallableId()
        }

    override val location: KaSymbolLocation
        get() = withValidityAssertion {
            when {
                backingPsi != null -> backingPsi.location
                firSymbol.origin == FirDeclarationOrigin.DynamicScope -> KaSymbolLocation.CLASS
                firSymbol.isLocal -> KaSymbolLocation.LOCAL
                firSymbol.containingClassLookupTag()?.classId == null -> KaSymbolLocation.TOP_LEVEL
                else -> KaSymbolLocation.CLASS
            }
        }

    override val modality: KaSymbolModality
        get() = withValidityAssertion { backingPsi?.kaSymbolModality ?: firSymbol.kaSymbolModality }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { backingPsi?.visibility ?: firSymbol.visibility }

    override fun createPointer(): KaSymbolPointer<KaNamedFunctionSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<KaNamedFunctionSymbol>()?.let { return it }

        when (val kind = location) {
            KaSymbolLocation.TOP_LEVEL -> KaFirTopLevelFunctionSymbolPointer(
                firSymbol.callableId,
                FirCallableSignature.createSignature(firSymbol),
                this
            )

            KaSymbolLocation.CLASS -> when (origin) {
                KaSymbolOrigin.JS_DYNAMIC -> KaFirDynamicFunctionSymbolPointer(name, this)
                else -> KaFirMemberFunctionSymbolPointer(
                    analysisSession.createOwnerPointer(this),
                    name,
                    FirCallableSignature.createSignature(firSymbol),
                    isStatic = firSymbol.isStatic,
                    originalSymbol = this
                )
            }

            KaSymbolLocation.LOCAL -> throw KaCannotCreateSymbolPointerForLocalLibraryDeclarationException(
                callableId?.toString() ?: name.asString()
            )

            else -> throw KaUnsupportedSymbolLocation(this::class, kind)
        }
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}
