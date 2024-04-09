/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirSingleResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.asResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.tryCollectDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkDeclarationStatusIsResolved
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirStatusResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.StatusComputationSession
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.utils.SmartSet

internal object LLFirStatusLazyResolver : LLFirLazyResolver(FirResolvePhase.STATUS) {
    override fun createTargetResolver(target: LLFirResolveTarget): LLFirTargetResolver = LLFirStatusTargetResolver(
        target = target,
        resolveMode = target.resolveMode(),
    )

    override fun phaseSpecificCheckIsResolved(target: FirElementWithResolveState) {
        if (target !is FirMemberDeclaration) return
        checkDeclarationStatusIsResolved(target)
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
}

private fun LLFirResolveTarget.resolveMode(): StatusResolveMode = when (this) {
    is LLFirSingleResolveTarget -> when (target) {
        is FirClassLikeDeclaration -> StatusResolveMode.OnlyTarget
        else -> StatusResolveMode.AllCallables
    }

    else -> StatusResolveMode.AllCallables
}

private class LLStatusComputationSession : StatusComputationSession() {
    val useSiteSessions: List<LLFirSession> get() = _useSiteSessions
    private val _useSiteSessions: MutableList<LLFirSession> = mutableListOf<LLFirSession>()

    inline fun withClassSession(regularClass: FirClass, action: () -> Unit) {
        val newSession = regularClass.llFirSession.takeUnless { it == useSiteSessions.lastOrNull() }
        try {
            newSession?.let(_useSiteSessions::add)
            action()
        } finally {
            newSession?.let { _useSiteSessions.removeLast() }
        }
    }
}

/**
 * This resolver is responsible for [STATUS][FirResolvePhase.STATUS] phase.
 *
 * This resolver:
 * - Transforms modality, visibility, and modifiers for [member declarations][FirMemberDeclaration].
 *
 * Special rules:
 * - First resolves outer classes to this phase.
 * - First resolves all members of super types for non-[FirClassLikeDeclaration] declarations.
 * - [Searches][org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirStatusTargetResolver.Transformer.superTypeToSymbols]
 *   super types not only in the declaration site session, but also in the call site session to resolve `expect` declaration first.
 *
 * @see FirStatusResolveTransformer
 * @see FirResolvePhase.STATUS
 */
private class LLFirStatusTargetResolver(
    target: LLFirResolveTarget,
    private val statusComputationSession: LLStatusComputationSession = LLStatusComputationSession(),
    private val resolveMode: StatusResolveMode,
) : LLFirTargetResolver(target, FirResolvePhase.STATUS) {
    private val transformer = Transformer(resolveTargetSession, resolveTargetScopeSession)

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withContainingRegularClass(firClass: FirRegularClass, action: () -> Unit) {
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

        else -> false
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
            is FirRegularClass -> error("should be resolved in doResolveWithoutLock")
            is FirFile, is FirScript -> {}
            else -> target.transformSingle(transformer, data = null)
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
                withContainingDeclaration(firClass) {
                    resolveCallableMembers(firClass)
                }

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
        val computationSession: LLStatusComputationSession get() = this@LLFirStatusTargetResolver.statusComputationSession

        override fun FirDeclaration.needResolveMembers(): Boolean = false
        override fun FirDeclaration.needResolveNestedClassifiers(): Boolean = false

        override fun transformClass(klass: FirClass, data: FirResolvedDeclarationStatus?): FirStatement {
            return klass
        }

        override fun forceResolveStatusesOfSupertypes(regularClass: FirClass) {
            computationSession.withClassSession(regularClass) {
                super.forceResolveStatusesOfSupertypes(regularClass)
            }
        }

        override fun superTypeToSymbols(typeRef: FirTypeRef): Collection<FirClassifierSymbol<*>> {
            val type = typeRef.coneType
            return SmartSet.create<FirClassifierSymbol<*>>().apply {
                // Resolution order: from declaration site to use site
                for (useSiteSession in computationSession.useSiteSessions.asReversed()) {
                    type.toSymbol(useSiteSession)?.let(::add)
                }
            }
        }

        override fun resolveClassForSuperType(regularClass: FirRegularClass): Boolean {
            val target = regularClass.tryCollectDesignation()?.asResolveTarget() ?: return false
            val resolver = LLFirStatusTargetResolver(
                target,
                computationSession,
                resolveMode = resolveMode,
            )

            resolver.resolveDesignation()
            return true
        }
    }
}
