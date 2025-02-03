/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.getAllowedPsi
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaCannotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaAnonymousFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration

internal class KaFirAnonymousFunctionSymbol private constructor(
    override val backingPsi: KtFunction?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirAnonymousFunctionSymbol>,
) : KaAnonymousFunctionSymbol(), KaFirKtBasedSymbol<KtFunction, FirAnonymousFunctionSymbol> {
    init {
        when (backingPsi) {
            null, is KtFunctionLiteral -> {}
            is KtNamedFunction -> {
                require(backingPsi.isAnonymous)
            }
        }
    }

    constructor(declaration: KtFunction, session: KaFirSession) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
    )

    constructor(symbol: FirAnonymousFunctionSymbol, session: KaFirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? KtFunction,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val psi: PsiElement? get() = withValidityAssertion { backingPsi ?: firSymbol.fir.getAllowedPsi() }
    override val annotations: KaAnnotationList get() = withValidityAssertion { psiOrSymbolAnnotationList() }
    override val returnType: KaType get() = withValidityAssertion { createReturnType() }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion {
            KaFirReceiverParameterSymbol.create(
                // We cannot use KtFunctionLiteral as the backing PSI as it doesn't receiver parameter in the code
                backingPsi?.takeIf { it is KtNamedFunction },
                analysisSession,
                this,
            )
        }

    override val contextReceivers: List<KaContextReceiver>
        get() = withValidityAssertion { createContextReceivers() }

    override val contextParameters: List<KaContextParameterSymbol>
        get() = withValidityAssertion {
            createKaContextParameters() ?: firSymbol.createKaContextParameters(builder)
        }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS.visibility }

    override val valueParameters: List<KaValueParameterSymbol>
        get() = withValidityAssertion {
            // lambda may have an implicit argument, so we cannot check it by PSI
            if (backingPsi is KtNamedFunction || backingPsi is KtFunctionLiteral && backingPsi.arrow != null) {
                createKaValueParameters()?.let { return it }
            }

            firSymbol.createKtValueParameters(builder)
        }

    override val isExtension: Boolean
        get() = withValidityAssertion { backingPsi?.isExtensionDeclaration() ?: firSymbol.isExtension }

    override fun createPointer(): KaSymbolPointer<KaAnonymousFunctionSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<KaAnonymousFunctionSymbol>()
            ?: throw KaCannotCreateSymbolPointerForLocalLibraryDeclarationException(this::class)
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}
