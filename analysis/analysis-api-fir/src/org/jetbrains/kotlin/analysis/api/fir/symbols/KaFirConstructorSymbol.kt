/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirConstructorSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirTypeAliasedConstructorMemberPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.fir.visibilityByModifiers
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.FirCallableSignature
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.utils.hasStableParameterNames
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.isExpectDeclaration

internal class KaFirConstructorSymbol private constructor(
    override val backingPsi: KtConstructor<*>?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirConstructorSymbol>,
) : KaConstructorSymbol(), KaFirKtBasedSymbol<KtConstructor<*>, FirConstructorSymbol> {
    constructor(declaration: KtConstructor<*>, session: KaFirSession) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
    )

    constructor(symbol: FirConstructorSymbol, session: KaFirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? KtConstructor<*>,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val psi: PsiElement? get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val returnType: KaType get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val valueParameters: List<KaValueParameterSymbol>
        get() = withValidityAssertion {
            createKaValueParameters() ?: firSymbol.createKtValueParameters(builder)
        }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null) {
                true
            } else {
                firSymbol.fir.hasStableParameterNames
            }
        }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { backingPsi?.visibilityByModifiers ?: firSymbol.visibility }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            psiOrSymbolAnnotationList()
        }

    override val containingClassId: ClassId?
        get() = withValidityAssertion {
            backingPsi?.getContainingClassOrObject()?.getClassId()
                ?: firSymbol.typeAliasConstructorInfo?.typeAliasSymbol?.classId?.takeUnless { it.isLocal }
                ?: firSymbol.containingClassLookupTag()?.classId?.takeUnless { it.isLocal }
        }

    override val isPrimary: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null) {
                backingPsi is KtPrimaryConstructor
            } else {
                firSymbol.isPrimary
            }
        }

    override val isActual: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.ACTUAL_KEYWORD) ?: firSymbol.isActual }

    override val isExpect: Boolean
        get() = withValidityAssertion { backingPsi?.isExpectDeclaration() ?: firSymbol.isExpect }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion {
            with(analysisSession) {
                backingPsi?.getContainingClassOrObject()?.symbol?.typeParameters ?: firSymbol.createKtTypeParameters(builder)
            }
        }

    override fun createPointer(): KaSymbolPointer<KaConstructorSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<KaConstructorSymbol>()?.let { return it }

        if (firSymbol.isTypeAliasedConstructor) {
            KaFirTypeAliasedConstructorMemberPointer(
                analysisSession.createOwnerPointer(this),
                FirCallableSignature.createSignature(firSymbol),
                this
            )
        } else {
            KaFirConstructorSymbolPointer(
                analysisSession.createOwnerPointer(this),
                isPrimary,
                FirCallableSignature.createSignature(firSymbol),
                this
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        return psiOrSymbolEquals(other)
    }

    override fun hashCode(): Int {
        return psiOrSymbolHashCode()
    }
}
