/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.withFileAnalysisExceptionWrapping

abstract class FirAbstractPhaseTransformer<D>(
    protected val baseTransformerPhase: FirResolvePhase
) : FirDefaultTransformer<D>() {

    open val transformerPhase get() = baseTransformerPhase

    abstract val session: FirSession

    init {
        assert(baseTransformerPhase != FirResolvePhase.RAW_FIR) {
            "Raw FIR building shouldn't be done in phase transformer"
        }
    }

    override fun transformFile(file: FirFile, data: D): FirFile {
        checkSessionConsistency(file)
        return withFileAnalysisExceptionWrapping(file) {
            super.transformFile(file, data)
        }
    }

    protected fun checkSessionConsistency(file: FirFile) {
        assert(session === file.moduleData.session) {
            "File ${file.name} and transformer ${this::class} have inconsistent sessions"
        }
    }
}
