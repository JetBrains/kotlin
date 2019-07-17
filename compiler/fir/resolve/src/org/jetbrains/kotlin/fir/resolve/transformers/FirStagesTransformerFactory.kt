/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirStagesTransformerFactory(session: FirSession) {


    val resolveStages: List<FirResolveStage> = listOf(
        ImportResolveStage,
        SuperTypeResolveStage,
        TypeResolveStage,
        StatusResolveStage,
        ImplicitTypeBodyResolveStage(session),
        BodyResolveStage(session)
    )

    val stageCount = resolveStages.size

    fun createStageTransformer(stage: Int): FirTransformer<Nothing?> {
        return resolveStages[stage].createTransformer()
    }

    fun processFiles(files: List<FirFile>) {
        for (resolveStage in resolveStages) {
            val transformer = resolveStage.createTransformer()
            for (firFile in files) {
                firFile.transform<FirFile, Nothing?>(transformer, null)
            }
        }
    }
}

sealed class FirResolveStage() {
    abstract val isParallel: Boolean
    abstract fun createTransformer(): FirTransformer<Nothing?>
}

object ImportResolveStage : FirResolveStage() {
    override val isParallel: Boolean
        get() = true

    override fun createTransformer(): FirTransformer<Nothing?> {
        return FirImportResolveTransformer()
    }
}

object SuperTypeResolveStage : FirResolveStage() {
    override val isParallel: Boolean
        get() = false

    override fun createTransformer(): FirTransformer<Nothing?> {
        return FirSupertypeResolverTransformer()
    }
}


object TypeResolveStage : FirResolveStage() {
    override val isParallel: Boolean
        get() = true

    override fun createTransformer(): FirTransformer<Nothing?> {
        return FirTypeResolveTransformer()
    }
}

object StatusResolveStage : FirResolveStage() {
    override val isParallel: Boolean
        get() = true

    override fun createTransformer(): FirTransformer<Nothing?> {
        return FirStatusResolveTransformer()
    }
}

class ImplicitTypeBodyResolveStage(val session: FirSession) : FirResolveStage() {
    override val isParallel: Boolean
        get() = false

    override fun createTransformer(): FirTransformer<Nothing?> {
        return FirImplicitTypeBodyResolveTransformerAdapter(session)
    }
}

class BodyResolveStage(val session: FirSession) : FirResolveStage() {
    override val isParallel: Boolean
        get() = true

    override fun createTransformer(): FirTransformer<Nothing?> {
        return FirBodyResolveTransformerAdapter(session)
    }
}