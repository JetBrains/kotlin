/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import java.util.*

/**
 * Maps [FirResolvePhase] to the associated [LLFirTargetResolver].
 */
internal object LLFirLazyPhaseResolverByPhase {
    private val byPhase = EnumMap<FirResolvePhase, LLFirLazyResolver>(FirResolvePhase::class.java).apply {
        this[FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS] = LLFirCompilerAnnotationsLazyResolver
        this[FirResolvePhase.COMPANION_GENERATION] = LLFirGeneratedCompanionObjectLazyResolver
        this[FirResolvePhase.SUPER_TYPES] = LLFirSupertypeLazyResolver
        this[FirResolvePhase.SEALED_CLASS_INHERITORS] = LLFirSealedClassInheritorsLazyResolver
        this[FirResolvePhase.TYPES] = LLFirTypeLazyResolver
        this[FirResolvePhase.STATUS] = LLFirStatusLazyResolver
        this[FirResolvePhase.EXPECT_ACTUAL_MATCHING] = LLFirExpectActualMatcherLazyResolver
        this[FirResolvePhase.CONTRACTS] = LLFirContractsLazyResolver
        this[FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE] = LLFirImplicitTypesLazyResolver
        this[FirResolvePhase.CONSTANT_EVALUATION] = LLFirConstantEvaluationLazyResolver
        this[FirResolvePhase.ANNOTATION_ARGUMENTS] = LLFirAnnotationArgumentsLazyResolver
        this[FirResolvePhase.BODY_RESOLVE] = LLFirBodyLazyResolver
    }

    fun getByPhase(phase: FirResolvePhase): LLFirLazyResolver = byPhase.getValue(phase)
}
