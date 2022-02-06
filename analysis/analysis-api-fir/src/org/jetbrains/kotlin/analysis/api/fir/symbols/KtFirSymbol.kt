/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirModuleResolveState
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.originalIfFakeOverride
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.scopes.impl.importedFromObjectData
import org.jetbrains.kotlin.fir.scopes.impl.originalForWrappedIntegerOperator
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

internal interface KtFirSymbol<out S : FirBasedSymbol<*>> : KtSymbol, ValidityTokenOwner {
    val firSymbol: S

    abstract val resolveState: LLFirModuleResolveState

    override val origin: KtSymbolOrigin get() = firSymbol.fir.ktSymbolOrigin()
}

internal fun KtFirSymbol<*>.symbolEquals(other: Any?): Boolean {
    if (other !is KtFirSymbol<*>) return false
    return this.firSymbol == other.firSymbol
}

internal fun KtFirSymbol<*>.symbolHashCode(): Int = firSymbol.hashCode()

internal tailrec fun FirDeclaration.ktSymbolOrigin(): KtSymbolOrigin = when (origin) {
    FirDeclarationOrigin.Source -> {
        when (source?.kind) {
            KtFakeSourceElementKind.ImplicitConstructor,
            KtFakeSourceElementKind.DataClassGeneratedMembers,
            KtFakeSourceElementKind.EnumGeneratedDeclaration,
            KtFakeSourceElementKind.ItLambdaParameter -> KtSymbolOrigin.SOURCE_MEMBER_GENERATED

            else -> KtSymbolOrigin.SOURCE
        }
    }
    FirDeclarationOrigin.Precompiled -> KtSymbolOrigin.SOURCE
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
    FirDeclarationOrigin.ImportedFromObject -> {
        val importedFromObjectData = (this as FirCallableDeclaration).importedFromObjectData
            ?: error("Declaration has ImportedFromObject origin, but no importedFromObjectData present")

        importedFromObjectData.original.ktSymbolOrigin()
    }
    FirDeclarationOrigin.WrappedIntegerOperator -> {
        val original = (this as FirSimpleFunction).originalForWrappedIntegerOperator?.fir
            ?: error("Declaration has WrappedIntegerOperator origin, but no originalForWrappedIntegerOperator present")

        original.ktSymbolOrigin()
    }
    is FirDeclarationOrigin.Plugin -> KtSymbolOrigin.PLUGIN
    else -> {
        val overridden = (this as? FirCallableDeclaration)?.originalIfFakeOverride()
            ?: throw InvalidFirDeclarationOriginForSymbol(this)
        overridden.ktSymbolOrigin()
    }
}


class InvalidFirDeclarationOriginForSymbol(declaration: FirDeclaration) :
    IllegalStateException("Invalid FirDeclarationOrigin ${declaration.origin::class.simpleName} for ${declaration.render()}")
