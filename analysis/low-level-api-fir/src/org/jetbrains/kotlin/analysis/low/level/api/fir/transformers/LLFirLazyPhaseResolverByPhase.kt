/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import java.util.*

internal object LLFirLazyPhaseResolverByPhase {
    private val byPhase = EnumMap<FirResolvePhase, LLFirLazyPhaseResolver>(FirResolvePhase::class.java).apply {
        this[FirResolvePhase.COMPANION_GENERATION] = LLFirDesignatedGeneratedCompanionObjectResolvePhaseResolver
        this[FirResolvePhase.SUPER_TYPES] = LLFirDesignatedSupertypeResolverPhaseResolver
        this[FirResolvePhase.TYPES] = LLFirDesignatedTypeResolverPhaseResolver
        this[FirResolvePhase.STATUS] = LLFirDesignatedStatusResolvePhaseResolver
        this[FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS] = LLFirDesignatedAnnotationsResolveTransformed
        this[FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS] = LLFirDesignatedAnnotationArgumentsResolvePhaseResolver
        this[FirResolvePhase.CONTRACTS] = LLFirDesignatedContractsResolvePhaseResolver
        this[FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE] = LLFirDesignatedImplicitTypesPhaseResolver
        this[FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING] = LLFirDesignatedAnnotationArgumentsMappingPhaseResolver
        this[FirResolvePhase.BODY_RESOLVE] = LLFirDesignatedBodyResolvePhaseResolver
        this[FirResolvePhase.EXPECT_ACTUAL_MATCHING] = LLFirDesignatedExpectActualMatcherPhaseResolver
    }

    fun getByPhase(phase: FirResolvePhase): LLFirLazyPhaseResolver = byPhase.getValue(phase)

    fun getByPhaseIfExists(phase: FirResolvePhase): LLFirLazyPhaseResolver? = byPhase[phase]
}
