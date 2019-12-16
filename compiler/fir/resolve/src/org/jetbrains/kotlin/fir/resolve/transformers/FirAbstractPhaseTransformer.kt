/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer

abstract class FirAbstractPhaseTransformer<D>(
    val transformerPhase: FirResolvePhase
) : FirDefaultTransformer<D>() {

    abstract val session: FirSession

    init {
        assert(transformerPhase != FirResolvePhase.RAW_FIR) {
            "Raw FIR building shouldn't be done in phase transformer"
        }
    }

    open val <D> AbstractFirBasedSymbol<D>.phasedFir: D where D : FirDeclaration, D : FirSymbolOwner<D>
        get() {
            val requiredPhase = transformerPhase.requiredToLaunch
            return phasedFir(requiredPhase)
        }

    override fun transformFile(file: FirFile, data: D): CompositeTransformResult<FirFile> {
        file.replaceResolvePhase(transformerPhase)

        @Suppress("UNCHECKED_CAST")
        return super.transformFile(file, data) as CompositeTransformResult<FirFile>
    }

    override fun transformDeclaration(declaration: FirDeclaration, data: D): CompositeTransformResult<FirDeclaration> {
        declaration.replaceResolvePhase(transformerPhase)

        return super.transformDeclaration(declaration, data)
    }
}

fun FirFile.runResolve(toPhase: FirResolvePhase, fromPhase: FirResolvePhase = FirResolvePhase.RAW_FIR) {
    var currentPhase = fromPhase
    while (currentPhase < toPhase) {
        currentPhase = currentPhase.next
        val phaseTransformer = currentPhase.createTransformerByPhase()
        transform<FirFile, Nothing?>(phaseTransformer, null)
    }
}