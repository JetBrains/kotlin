/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector

internal object LazyTransformerFactory {
    fun createLazyTransformer(
        phase: FirResolvePhase,
        designation: FirDesignationWithFile,
        scopeSession: ScopeSession,
        lockProvider: LLFirLockProvider,
        towerDataContextCollector: FirTowerDataContextCollector?
    ): LLFirLazyTransformer = when (phase) {
        FirResolvePhase.COMPANION_GENERATION -> LLFirDesignatedGeneratedCompanionObjectResolveTransformer(
            designation = designation,
            session = designation.firFile.moduleData.session,
        )
        FirResolvePhase.SEALED_CLASS_INHERITORS -> LLFirLazyTransformer.DUMMY
        FirResolvePhase.SUPER_TYPES -> LLFirDesignatedSupertypeResolverTransformer(
            designation = designation,
            session = designation.firFile.moduleData.session,
            scopeSession = scopeSession,
            lockProvider = lockProvider
        )
        FirResolvePhase.TYPES -> LLFirDesignatedTypeResolverTransformer(
            designation,
            designation.firFile.moduleData.session,
            scopeSession,
        )
        FirResolvePhase.STATUS -> LLFirDesignatedStatusResolveTransformer(
            designation,
            designation.firFile.moduleData.session,
            scopeSession,
        )
        FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS -> LLFirDesignatedAnnotationsResolveTransformed(
            designation,
            designation.firFile.moduleData.session,
            scopeSession,
        )
        FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS -> LLFirDesignatedAnnotationArgumentsResolveTransformer(
            designation,
            designation.firFile.moduleData.session,
            scopeSession,
        )
        FirResolvePhase.CONTRACTS -> LLFirDesignatedContractsResolveTransformer(
            designation,
            designation.firFile.moduleData.session,
            scopeSession,
        )
        FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> LLFirDesignatedImplicitTypesTransformer(
            designation,
            designation.firFile.moduleData.session,
            scopeSession,
            towerDataContextCollector
        )
        FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING -> LLFirDesignatedAnnotationArgumentsMappingTransformer(
            designation,
            designation.firFile.moduleData.session,
            scopeSession,
        )
        FirResolvePhase.BODY_RESOLVE -> LLFirDesignatedBodyResolveTransformer(
            designation,
            designation.firFile.moduleData.session,
            scopeSession,
            towerDataContextCollector
        )
        FirResolvePhase.EXPECT_ACTUAL_MATCHING -> LLFirDesignatedExpectActualMatcherTransformer(
            designation,
            designation.firFile.moduleData.session,
            scopeSession
        )
        FirResolvePhase.RAW_FIR -> error("Non-lazy phase $phase")
        FirResolvePhase.IMPORTS -> error("Non-lazy phase $phase")
    }
}
