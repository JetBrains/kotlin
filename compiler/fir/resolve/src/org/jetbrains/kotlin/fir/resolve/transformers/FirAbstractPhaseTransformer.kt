/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
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

    override fun transformFile(file: FirFile, data: D): CompositeTransformResult<FirFile> {
        checkSessionConsistency(file)
        file.replaceResolvePhase(transformerPhase)

        @Suppress("UNCHECKED_CAST")
        return super.transformFile(file, data) as CompositeTransformResult<FirFile>
    }

    override fun transformDeclaration(declaration: FirDeclaration, data: D): CompositeTransformResult<FirDeclaration> {
        declaration.replaceResolvePhase(transformerPhase)

        return super.transformDeclaration(declaration, data)
    }

    protected fun checkSessionConsistency(file: FirFile) {
        assert(session === file.session) {
            "File ${file.name} and transformer ${this::class} have inconsistent sessions"
        }
    }
}

fun FirFile.runResolve(toPhase: FirResolvePhase, fromPhase: FirResolvePhase = FirResolvePhase.RAW_FIR) {
    val scopeSession = ScopeSession()
    var currentPhase = fromPhase
    while (currentPhase < toPhase) {
        currentPhase = currentPhase.next
        val phaseProcessor = currentPhase.createTransformerBasedProcessorByPhase(session, scopeSession)
        phaseProcessor.processFile(this)
    }
}
