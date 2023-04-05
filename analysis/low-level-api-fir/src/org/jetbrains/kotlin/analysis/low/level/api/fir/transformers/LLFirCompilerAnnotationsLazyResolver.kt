/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.asResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationsComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirCompilerRequiredAnnotationsResolveTransformer
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase

internal object LLFirCompilerAnnotationsLazyResolver : LLFirLazyResolver(FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS) {
    override fun resolve(
        target: LLFirResolveTarget,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        val resolver = LLFirCompilerRequiredAnnotationsTargetResolver(target, lockProvider, session, scopeSession)
        resolver.resolveDesignation()
    }

    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(target, resolverPhase, updateForLocalDeclarations = false)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(resolverPhase)
        checkNestedDeclarationsAreResolved(target)
        // TODO add proper check that COMPILER_REQUIRED_ANNOTATIONS are resolved
    }
}

private class LLFirCompilerRequiredAnnotationsTargetResolver(
    target: LLFirResolveTarget,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
    computationSession: LLFirCompilerRequiredAnnotationsComputationSession? = null,
) : LLFirTargetResolver(target, lockProvider, FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS, isJumpingPhase = true) {
    inner class LLFirCompilerRequiredAnnotationsComputationSession : CompilerRequiredAnnotationsComputationSession() {
        override fun resolveAnnotationSymbol(symbol: FirRegularClassSymbol, scopeSession: ScopeSession) {
            val regularClass = symbol.fir
            if (regularClass.resolvePhase >= resolverPhase) return

            symbol.lazyResolveToPhase(resolverPhase.previous)
            val designation = regularClass.collectDesignationWithFile().asResolveTarget()
            val resolver = LLFirCompilerRequiredAnnotationsTargetResolver(
                designation,
                lockProvider,
                designation.target.llFirSession,
                scopeSession,
                this,
            )

            resolver.resolveDesignation()
        }
    }

    private val transformer = FirCompilerRequiredAnnotationsResolveTransformer(
        session,
        scopeSession,
        computationSession ?: LLFirCompilerRequiredAnnotationsComputationSession(),
    )

    override fun withFile(firFile: FirFile, action: () -> Unit) {
        transformer.annotationTransformer.withFileAndFileScopes(firFile) {
            action()
        }
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withRegularClassImpl(firClass: FirRegularClass, action: () -> Unit) {
        transformer.annotationTransformer.withRegularClass(firClass) {
            action()
        }
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        FirLazyBodiesCalculator.calculateCompilerAnnotations(target)
        when (target) {
            is FirTypeAlias -> {
                transformer.transformTypeAlias(target, null)
            }
            is FirRegularClass -> {
                transformer.annotationTransformer.resolveRegularClass(
                    target,
                    transformChildren = {
                        target.transformSuperTypeRefs(transformer.annotationTransformer, null)
                    },
                    afterChildrenTransform = {
                        transformer.annotationTransformer.calculateDeprecations(target)
                    }
                )
            }
        }
    }
}