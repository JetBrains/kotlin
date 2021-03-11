/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.originalIfFakeOverride
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.FirRefWithValidityCheck
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbolOrigin

internal interface KtFirSymbol<F : FirDeclaration> : KtSymbol, ValidityTokenOwner {
    val firRef: FirRefWithValidityCheck<F>

    override val origin: KtSymbolOrigin get() = firRef.withFir { it.ktSymbolOrigin() }
}


private tailrec fun FirDeclaration.ktSymbolOrigin(): KtSymbolOrigin = when (origin) {
    FirDeclarationOrigin.Source -> {
        if (source?.kind == FirFakeSourceElementKind.DataClassGeneratedMembers
            || source?.kind == FirFakeSourceElementKind.EnumGeneratedDeclaration
        ) {
            KtSymbolOrigin.SOURCE_MEMBER_GENERATED
        } else KtSymbolOrigin.SOURCE
    }
    FirDeclarationOrigin.Library, FirDeclarationOrigin.BuiltIns -> KtSymbolOrigin.LIBRARY
    FirDeclarationOrigin.Java -> KtSymbolOrigin.JAVA
    FirDeclarationOrigin.SamConstructor -> KtSymbolOrigin.SAM_CONSTRUCTOR
    FirDeclarationOrigin.Enhancement -> KtSymbolOrigin.JAVA
    FirDeclarationOrigin.IntersectionOverride -> KtSymbolOrigin.INTERSECTION_OVERRIDE
    FirDeclarationOrigin.Delegated -> KtSymbolOrigin.DELEGATED
    FirDeclarationOrigin.Synthetic -> {
        when {
            this is FirSyntheticProperty -> KtSymbolOrigin.JAVA_SYNTHETIC_PROPERTY
            else -> throw InvalidFirDeclarationOriginForSymbol(this)
        }
    }

    else -> {
        val overridden = (this as? FirCallableDeclaration<*>)?.originalIfFakeOverride()
            ?: throw InvalidFirDeclarationOriginForSymbol(this)
        overridden.ktSymbolOrigin()
    }
}


class InvalidFirDeclarationOriginForSymbol(declaration: FirDeclaration) :
    IllegalStateException("Invalid FirDeclarationOrigin ${declaration.origin::class.simpleName} for ${declaration.render()}")
