/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaCannotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.KaAnonymousFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression
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
        lazyFirSymbol = lazyFirSymbol(declaration, analysisSession = session),
        analysisSession = session,
    )

    constructor(symbol: FirAnonymousFunctionSymbol, session: KaFirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? KtFunction,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val annotations: KaAnnotationList
        get() = entryPointImplementation {
            /**
             * If this anonymous function is a function literal, [backingPsi] is [KtFunctionLiteral].
             * However, annotations are placed on the enclosing [KtLambdaExpression], i.e.,
             * it has a [org.jetbrains.kotlin.psi.KtAnnotatedExpression] parent.
             *
             * So an adjusted PSI should be provided for the PSI-based optimization.
             * ```kotlin
             * @MyAnnotation {
             *     // Something
             * }
             * ```
             */
            val adjustedPsi = (backingPsi as? KtFunctionLiteral)?.parent as? KtLambdaExpression
            psiOrSymbolAnnotationList(adjustedPsi ?: backingPsi)
        }

    override val returnType: KaType get() = entryPointImplementation { createReturnType() }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = entryPointImplementation {
            KaFirReceiverParameterSymbol.create(
                // We cannot use KtFunctionLiteral as the backing PSI as it doesn't receiver parameter in the code
                backingPsi?.takeIf { it is KtNamedFunction },
                this,
            )
        }

    override val contextReceivers: List<KaContextReceiver>
        get() = entryPointImplementation { createContextReceivers() }

    override val contextParameters: List<KaContextParameterSymbol>
        get() = entryPointImplementation {
            createKaContextParameters() ?: firSymbol.createKaContextParameters()
        }

    override val valueParameters: List<KaValueParameterSymbol>
        get() = entryPointImplementation {
            // lambda may have an implicit argument, so we cannot check it by PSI
            if (backingPsi is KtNamedFunction || backingPsi is KtFunctionLiteral && backingPsi.arrow != null) {
                createKaValueParameters()?.let { return it }
            }

            firSymbol.createKtValueParameters()
        }

    override val isExtension: Boolean
        get() = entryPointImplementation { backingPsi?.isExtensionDeclaration() ?: firSymbol.isExtension }

    override fun createPointer(): KaSymbolPointer<KaAnonymousFunctionSymbol> = entryPointImplementation {
        psiBasedSymbolPointerOfTypeIfSource<KaAnonymousFunctionSymbol>()
            ?: throw KaCannotCreateSymbolPointerForLocalLibraryDeclarationException(this::class)
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}
