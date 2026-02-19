/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseContextParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFirContextReceiverBasedContextParameterSymbol private constructor(
    override val backingPsi: KtContextReceiver?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirValueParameterSymbol>,
) : KaContextParameterSymbol(), KaFirKtBasedSymbol<KtContextReceiver, FirValueParameterSymbol> {
    init {
        if (backingPsi != null) {
            requireNotNull(backingPsi.ownerDeclaration)
        }
    }

    constructor(contextReceiver: KtContextReceiver, session: KaFirSession) : this(
        backingPsi = contextReceiver,
        lazyFirSymbol = lazy(LazyThreadSafetyMode.PUBLICATION) {
            val declaration = contextReceiver.ownerDeclaration!!
            val firSymbol = declaration.resolveToFirSymbol(session.resolutionFacade)
            firSymbol.fir
                .contextParameters
                .find { it.psi == contextReceiver }
                ?.symbol
                ?: errorWithAttachment("Cannot find context receiver in FIR declaration") {
                    withFirSymbolEntry("symbol", firSymbol)
                    withPsiEntry("contextReceiver", contextReceiver)
                }
        },
        analysisSession = session,
    )

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val name: Name
        get() = withValidityAssertion {
            // Currently, library elements are representing both context receiver and context parameters,
            // so there is no way to distinguish between them.
            // And by default they should be unnamed
            ifSource {
                backingPsi?.name()?.let(Name::identifier) ?: SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
            } ?: firSymbol.name
        }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS.visibility }

    override val returnType: KaType
        get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { KaBaseEmptyAnnotationList(token) }

    override fun createPointer(): KaSymbolPointer<KaContextParameterSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<KaContextParameterSymbol>()?.let { return it }

        val ownerSymbol = with(analysisSession) { containingDeclaration }
            ?: error("Containing declaration is expected for a context parameter symbol")

        val parameters = ownerSymbol.firSymbol.fir.contextParameters
        return KaBaseContextParameterSymbolPointer(
            ownerPointer = analysisSession.createOwnerPointer(this),
            name = name,
            index = parameters.indexOf(firSymbol.fir),
            originalSymbol = this,
        )
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}

private val FirDeclaration.contextParameters: List<FirValueParameter>
    get() = when (this) {
        is FirCallableDeclaration -> contextParameters
        is FirRegularClass -> contextParameters
        else -> errorWithAttachment("Unexpected FIR ${this::class.simpleName}") {
            withFirEntry("declaration", this@contextParameters)
        }
    }