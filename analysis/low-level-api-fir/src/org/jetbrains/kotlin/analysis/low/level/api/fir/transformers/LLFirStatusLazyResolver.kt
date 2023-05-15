/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirSingleResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.asResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.tryCollectDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkDeclarationStatusIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirStatusResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.StatusComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.StatusComputationSession.StatusComputationStatus
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name

internal object LLFirStatusLazyResolver : LLFirLazyResolver(FirResolvePhase.STATUS) {
    override fun resolve(
        target: LLFirResolveTarget,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        val resolver = LLFirStatusTargetResolver(
            target = target,
            lockProvider = lockProvider,
            session = session,
            scopeSession = scopeSession,
            resolveMode = target.resolveMode(),
        )

        resolver.resolveDesignation()
    }

    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(target, resolverPhase, updateForLocalDeclarations = false)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        if (target !is FirAnonymousInitializer) {
            target.checkPhase(resolverPhase)
        }

        if (target is FirMemberDeclaration) {
            checkDeclarationStatusIsResolved(target)
        }

        checkNestedDeclarationsAreResolved(target)
    }
}

private sealed class StatusResolveMode(val resolveSupertypes: Boolean) {
    abstract fun shouldBeResolved(callableDeclaration: FirCallableDeclaration): Boolean

    object OnlyTarget : StatusResolveMode(resolveSupertypes = false) {
        override fun shouldBeResolved(callableDeclaration: FirCallableDeclaration): Boolean = false
    }

    object AllCallables : StatusResolveMode(resolveSupertypes = true) {
        override fun shouldBeResolved(callableDeclaration: FirCallableDeclaration): Boolean = true
    }

    class FunctionWithSpecificName(val name: Name) : StatusResolveMode(resolveSupertypes = true) {
        override fun shouldBeResolved(callableDeclaration: FirCallableDeclaration): Boolean {
            return callableDeclaration is FirSimpleFunction && callableDeclaration.name == name
        }
    }

    class PropertyWithSpecificName(val name: Name) : StatusResolveMode(resolveSupertypes = true) {
        override fun shouldBeResolved(callableDeclaration: FirCallableDeclaration): Boolean {
            return callableDeclaration is FirProperty && callableDeclaration.name == name
        }
    }
}

private fun LLFirResolveTarget.resolveMode(): StatusResolveMode = when (this) {
    is LLFirSingleResolveTarget -> when (target) {
        is FirClassLikeDeclaration -> StatusResolveMode.OnlyTarget
        else -> StatusResolveMode.AllCallables
    }

    else -> StatusResolveMode.AllCallables
}

private class LLFirStatusTargetResolver(
    target: LLFirResolveTarget,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
    private val statusComputationSession: StatusComputationSession = StatusComputationSession(),
    private val resolveMode: StatusResolveMode,
) : LLFirTargetResolver(target, lockProvider, FirResolvePhase.STATUS, isJumpingPhase = false) {
    private val transformer = Transformer(session, scopeSession)

    override fun withFile(firFile: FirFile, action: () -> Unit) {
        action()
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withRegularClassImpl(firClass: FirRegularClass, action: () -> Unit) {
        doResolveWithoutLock(firClass)
        transformer.storeClass(firClass) {
            action()
            firClass
        }

        transformer.statusComputationSession.endComputing(firClass)
    }

    private fun resolveClassTypeParameters(klass: FirClass) {
        klass.typeParameters.forEach { it.transformSingle(transformer, data = null) }
    }

    private fun resolveCallableMembers(klass: FirClass) {
        for (member in klass.declarations) {
            if (member !is FirCallableDeclaration || !resolveMode.shouldBeResolved(member)) continue

            // we need the types to be resolved here to compute visibility
            // implicit types will not be handled (see bug in the compiler KT-55446)
            member.lazyResolveToPhase(resolverPhase.previous)
            performResolve(member)
        }
    }

    override fun doResolveWithoutLock(target: FirElementWithResolveState): Boolean = when {
        target is FirRegularClass -> {
            if (transformer.statusComputationSession[target].requiresComputation) {
                target.lazyResolveToPhase(resolverPhase.previous)
                resolveClass(target)
            }

            true
        }

        target is FirSimpleFunction -> {
            performResolveWithOverriddenCallables(
                target,
                { transformer.statusResolver.getOverriddenFunctions(it, transformer.containingClass) },
                { element, overridden -> transformer.transformSimpleFunction(element, overridden) },
            )

            true
        }

        target is FirProperty -> {
            performResolveWithOverriddenCallables(
                target,
                { transformer.statusResolver.getOverriddenProperties(it, transformer.containingClass) },
                { element, overridden -> transformer.transformProperty(element, overridden) },
            )

            true
        }

        else -> {
            false
        }
    }

    private inline fun <T : FirCallableDeclaration> performResolveWithOverriddenCallables(
        target: T,
        getOverridden: (T) -> List<T>,
        crossinline transform: (T, List<T>) -> Unit,
    ) {
        if (target.resolvePhase >= resolverPhase) return
        val overriddenDeclarations = getOverridden(target)
        performCustomResolveUnderLock(target) {
            transform(target, overriddenDeclarations)
        }
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        when (target) {
            is FirRegularClass -> {
                error("should be resolved in doResolveWithoutLock")
            }
            else -> {
                target.transformSingle(transformer, data = null)
            }
        }
    }

    private fun resolveClass(firClass: FirRegularClass) {
        transformer.statusComputationSession.startComputing(firClass)

        if (resolveMode.resolveSupertypes) {
            transformer.forceResolveStatusesOfSupertypes(firClass)
        }

        performCustomResolveUnderLock(firClass) {
            transformer.transformClassStatus(firClass)
            transformer.transformValueClassRepresentation(firClass)
            transformer.storeClass(firClass) {
                resolveClassTypeParameters(firClass)
                firClass
            }
        }

        if (resolveMode.resolveSupertypes) {
            transformer.storeClass(firClass) {
                resolveCallableMembers(firClass)
                firClass
            }

            transformer.statusComputationSession.endComputing(firClass)
        } else {
            transformer.statusComputationSession.computeOnlyClassStatus(firClass)
        }
    }

    private inner class Transformer(
        session: FirSession,
        scopeSession: ScopeSession,
    ) : FirStatusResolveTransformer(session, scopeSession, statusComputationSession) {

        override fun FirDeclaration.needResolveMembers(): Boolean = false
        override fun FirDeclaration.needResolveNestedClassifiers(): Boolean = false

        override fun transformClass(klass: FirClass, data: FirResolvedDeclarationStatus?): FirStatement {
            return klass
        }

        override fun resolveClassForSuperType(regularClass: FirRegularClass): Boolean {
            if (regularClass.resolvePhase >= resolverPhase) {
                // We can avoid resolve in the case of all declarations in super class are already resolved
                val declarations = regularClass.declarations
                if (declarations.isNotEmpty() && declarations.all { it.resolvePhase >= resolverPhase }) {
                    return true
                }
            }

            val target = regularClass.tryCollectDesignationWithFile()?.asResolveTarget() ?: return false
            val resolver = LLFirStatusTargetResolver(
                target,
                lockProvider,
                session,
                scopeSession,
                statusComputationSession,
                resolveMode = resolveMode,
            )

            resolver.resolveDesignation()
            return true
        }
    }
}
