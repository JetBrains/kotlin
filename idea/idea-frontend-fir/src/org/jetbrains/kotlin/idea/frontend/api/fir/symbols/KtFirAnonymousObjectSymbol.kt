/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.superConeTypes
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.convertAnnotation
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.firRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtAnonymousObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtAnnotationCall
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.types.KtType

internal class KtFirAnonymousObjectSymbol(
    fir: FirAnonymousObject,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtAnonymousObjectSymbol(), KtFirSymbol<FirAnonymousObject> {

    override val firRef = firRef(fir, resolveState)
    override val symbolKind: KtSymbolKind = KtSymbolKind.LOCAL
    override val psi: PsiElement? by firRef.withFirAndCache { fir -> fir.findPsi(fir.session) }

    override val annotations: List<KtAnnotationCall> by firRef.withFirAndCache(FirResolvePhase.TYPES) {
        convertAnnotation(it)
    }

    override val superTypes: List<KtType> by firRef.withFirAndCache(FirResolvePhase.SUPER_TYPES) { fir ->
        fir.superConeTypes.map {
            builder.buildKtType(it)
        }
    }

    override fun createPointer(): KtSymbolPointer<KtAnonymousObjectSymbol> =
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)
            ?: throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException("Cannot create pointer for KtFirAnonymousObjectSymbol")
}