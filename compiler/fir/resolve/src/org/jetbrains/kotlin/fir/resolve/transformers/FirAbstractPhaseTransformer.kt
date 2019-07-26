/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.FirSymbolOwner
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer

abstract class FirAbstractPhaseTransformer<D>(
    protected val transformerPhase: FirResolvePhase
) : FirTransformer<D>() {

    abstract val session: FirSession

    init {
        assert(transformerPhase != FirResolvePhase.RAW_FIR) {
            "Raw FIR building shouldn't be done in phase transformer"
        }
    }

    val <D> AbstractFirBasedSymbol<D>.phasedFir: D where D : FirDeclaration, D : FirSymbolOwner<D>
        get() {
            val result = this.fir
            val requiredPhase = transformerPhase.prev
            val availablePhase = result.resolvePhase
            if (availablePhase < requiredPhase) {
                var resolvePhase = availablePhase.next
                val provider = FirProvider.getInstance(session)
                val containingFile = when (this) {
                    is ConeCallableSymbol -> provider.getFirCallableContainerFile(this)
                    is ConeClassLikeSymbol -> provider.getFirClassifierContainerFile(this)
                    else -> null
                } ?: throw AssertionError("Cannot get container file by symbol: $this (${result.render()})")
                do {
                    val phaseTransformer = resolvePhase.createTransformerByPhase()
                    containingFile.transform<FirFile, Nothing?>(phaseTransformer, null)
                    resolvePhase = resolvePhase.next
                } while (resolvePhase <= requiredPhase)
            }
            return result
        }

    override fun transformDeclaration(declaration: FirDeclaration, data: D): CompositeTransformResult<FirDeclaration> {
        declaration.resolvePhase = transformerPhase

        return super.transformDeclaration(declaration, data)
    }
}