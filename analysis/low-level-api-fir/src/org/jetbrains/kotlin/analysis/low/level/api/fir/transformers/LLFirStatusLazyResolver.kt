/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirSingleResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.asResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.session
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.tryCollectDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkDeclarationStatusIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkAnalysisReadiness
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.FirStatusResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.StatusComputationSession
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseWithCallableMembers
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.utils.SmartSet

internal object LLFirStatusLazyResolver : LLFirLazyResolver(FirResolvePhase.STATUS) {
    override fun createTargetResolver(target: LLFirResolveTarget): LLFirTargetResolver {
        val session = target.session
        val resolveMode = target.resolveMode()
        return LLFirStatusTargetResolver(
            target = target,
            resolveMode = resolveMode,
            statusComputationSession = LLStatusComputationSession(
                session,
                session.getScopeSession(),
                resolveMode,
            )
        )
    }

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

/**
 * This session is designed to be used during local classes resolution to
 * properly handle non-local classes in the hierarchy to not modify them by the CLI transformer.
 *
 * It is not enough to just resolve non-local classes to [FirResolvePhase.STATUS] due to classpath substitution.
 * Such non-local classes have to be processed via [LLStatusComputationSession.forceResolveStatusesOfSupertypes]
 * in the context of local use site.
 *
 * ### Example:
 * ```kotlin
 * // MODULE: dependency
 * // FILE: dependency.kt
 * package org.example
 *
 * interface Base
 *
 * abstract class Foo : Base
 *
 * // MODULE: usage(dependency)
 * // FILE: usage.kt
 * package org.example
 *
 * interface Base {
 *     fun bar()
 * }
 *
 * fun test() {
 *     abstract class FooImpl : Foo() {
 *         override fun bar() {}
 *     }
 * }
 * ```
 *
 * In this case, the entire hierarchy of `Foo` has to be resolved with the local use site session.
 *
 * @see LLStatusComputationSession
 * @see org.jetbrains.kotlin.fir.resolve.transformers.runStatusResolveForLocalClass
 */
internal class LLStatusComputationSessionLocalClassesAware(
    useSiteSession: FirSession,
    useSiteScopeSession: ScopeSession,
) : StatusComputationSession(useSiteSession, useSiteScopeSession) {
    override fun resolveClassForSuperType(regularClass: FirRegularClass): Boolean = if (regularClass.isLocal) {
        super.resolveClassForSuperType(regularClass)
    } else {
        // 1. Resolve the entire hierarchy for the non-local class (in the declaration-site context)
        regularClass.lazyResolveToPhaseWithCallableMembers(FirResolvePhase.STATUS)

        val statusComputationSession = LLStatusComputationSession(
            useSiteSession as LLFirSession,
            useSiteScopeSession,
            StatusResolveMode.AllCallables,
        )

        // 2. Resolve the entire hierarchy for the non-local class (in the use-site context)
        statusComputationSession.forceResolveStatusesOfSupertypes(regularClass)
        true
    }
}

private class LLStatusComputationSession(
    useSiteSession: LLFirSession,
    useSiteScopeSession: ScopeSession,
    val resolveMode: StatusResolveMode,
) : StatusComputationSession(useSiteSession, useSiteScopeSession) {
    private val useSiteSessions: MutableList<LLFirSession> = mutableListOf(useSiteSession)

    private inline fun withClassSession(regularClass: FirClass, action: () -> Unit) {
        val newSession = regularClass.llFirSession.takeUnless { it == useSiteSessions.lastOrNull() }
        try {
            newSession?.let(useSiteSessions::add)
            action()
        } finally {
            newSession?.let { useSiteSessions.removeLast() }
        }
    }

    override fun forceResolveStatusesOfSupertypes(regularClass: FirClass) {
        withClassSession(regularClass) {
            super.forceResolveStatusesOfSupertypes(regularClass)
        }
    }

    override fun superTypeToSymbols(typeRef: FirTypeRef): Collection<FirClassifierSymbol<*>> {
        val type = typeRef.coneType
        return SmartSet.create<FirClassifierSymbol<*>>().apply {
            // Resolution order: from declaration site to use site
            for (useSiteSession in useSiteSessions.asReversed()) {
                type.toSymbol(useSiteSession)?.let(::add)
            }
        }
    }

    override fun resolveClassForSuperType(regularClass: FirRegularClass): Boolean {
        val target = regularClass.tryCollectDesignation()?.asResolveTarget() ?: return false
        val resolver = LLFirStatusTargetResolver(
            target,
            resolveMode = resolveMode,
            this,
        )

        resolver.resolveDesignation()
        return true
    }

    override fun additionalSuperTypes(regularClass: FirClass): List<FirTypeRef> {
        // Stdlib classes may be mapped to Java classes which may have a different supertype set.
        // E.g., a 'kotlin.collections.Collection' is immutable, while 'java.util.Collection' is mutable (it implements 'MutableIterable').
        // The logic here is only applicable to the Kotlin project where stdlib comes in sources.
        val shouldResolveJavaSupertypeCallables = regularClass is FirRegularClass
                && regularClass.origin.isLazyResolvable
                && regularClass.classId.packageFqName.startsWith(StandardClassIds.BASE_KOTLIN_PACKAGE)
                && regularClass.moduleData.platform.isJvm()

        if (!shouldResolveJavaSupertypeCallables) {
            return emptyList()
        }

        val fqName = regularClass.classId.asSingleFqName().toUnsafe()
        val javaClassFqName = JavaToKotlinClassMap.mapKotlinToJava(fqName) ?: return emptyList()
        val javaSymbol = javaClassFqName.toSymbol(useSiteSession) as? FirClassSymbol ?: return emptyList()
        return javaSymbol.resolvedSuperTypeRefs
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
 * - [Searches][org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLStatusComputationSession.superTypeToSymbols]
 *   super types not only in the declaration site session, but also in the call site session to resolve `expect` declaration first.
 *
 * @see FirStatusResolveTransformer
 * @see FirResolvePhase.STATUS
 */
private class LLFirStatusTargetResolver(
    target: LLFirResolveTarget,
    private val resolveMode: StatusResolveMode,
    statusComputationSession: LLStatusComputationSession,
) : LLFirTargetResolver(target, FirResolvePhase.STATUS) {
    private val transformer = Transformer(statusComputationSession)

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

    override fun doResolveWithoutLock(target: FirElementWithResolveState): Boolean = when (target) {
        is FirRegularClass -> {
            if (transformer.statusComputationSession[target].requiresComputation) {
                target.lazyResolveToPhase(resolverPhase.previous)
                resolveClass(target)
            }

            true
        }

        is FirNamedFunction -> {
            performResolveWithOverriddenCallables(
                target,
                { transformer.statusResolver.getOverriddenFunctions(it, transformer.containingClass) },
                { element, overridden -> transformer.transformNamedFunction(element, overridden) },
            )

            true
        }

        is FirProperty -> {
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
        if (checkAnalysisReadiness(target, containingDeclarations, resolverPhase)) return

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
            transformer.statusComputationSession.forceResolveStatusesOfSupertypes(firClass)
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

    private class Transformer(statusComputationSession: LLStatusComputationSession) :
        FirStatusResolveTransformer(statusComputationSession) {
        override fun FirDeclaration.needResolveMembers(): Boolean = false
        override fun FirDeclaration.needResolveNestedClassifiers(): Boolean = false

        override fun transformClass(klass: FirClass, data: FirResolvedDeclarationStatus?): FirStatement {
            return klass
        }
    }
}
