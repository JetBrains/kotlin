/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.KtFirBackingFieldSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.firRef
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtBackingFieldSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtTypeAndAnnotations
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer

internal class KtFirBackingFieldSymbol(
    propertyFir: FirProperty,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    _builder: KtSymbolByFirBuilder
) : KtBackingFieldSymbol(){
    private val builder by weakRef(_builder)
    private val propertyFirRef = firRef(propertyFir, resolveState)

    override val annotatedType: KtTypeAndAnnotations by cached {
        propertyFirRef.returnTypeAndAnnotations(FirResolvePhase.TYPES, builder)
    }

    override val owningProperty: KtKotlinPropertySymbol by propertyFirRef.withFirAndCache { fir ->
        builder.variableLikeBuilder.buildPropertySymbol(fir)
    }

    override fun createPointer(): KtSymbolPointer<KtBackingFieldSymbol> {
        return KtFirBackingFieldSymbolPointer(owningProperty.createPointer())
    }
}