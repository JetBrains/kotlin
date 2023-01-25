/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import java.util.*

internal object LLFirLazyPhaseResolverByPhase {
    private val byPhase = EnumMap<FirResolvePhase, LLFirLazyResolver>(FirResolvePhase::class.java).apply {
        this[FirResolvePhase.COMPANION_GENERATION] = LLFirGeneratedCompanionObjectLazyResolver
        this[FirResolvePhase.SUPER_TYPES] = LLFirSupertypeLazyResolver
        this[FirResolvePhase.TYPES] = LLFirTypeLazyResolver
        this[FirResolvePhase.STATUS] = LLFirStatusLazyResolver
        this[FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS] = LLFirCompilerAnnotationsLazyResolver
        this[FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS] = LLFirAnnotationArgumentsLazyResolver
        this[FirResolvePhase.CONTRACTS] = LLFirContractsLazyResolver
        this[FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE] = LLFirImplicitTypesLazyResolver
        this[FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING] = LLFirAnnotationArgumentMappingLazyResolver
        this[FirResolvePhase.BODY_RESOLVE] = LLFirBodyLazyResolver
        this[FirResolvePhase.EXPECT_ACTUAL_MATCHING] = LLFirExpectActualMatcherLazyResolver
        this[FirResolvePhase.SEALED_CLASS_INHERITORS] = LLFirSealedClassInheritorsLazyResolver
    }

    fun getByPhase(phase: FirResolvePhase): LLFirLazyResolver = byPhase.getValue(phase)
}
