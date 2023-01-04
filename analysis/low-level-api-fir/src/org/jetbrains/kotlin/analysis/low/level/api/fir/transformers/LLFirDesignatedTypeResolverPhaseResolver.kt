/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirDesignationToResolve
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkReceiverTypeRefIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkReturnTypeRefIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkTypeRefIsResolved
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptor
import org.jetbrains.kotlin.fir.resolve.transformers.FirTypeResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector

internal object LLFirDesignatedTypeResolverPhaseResolver : LLFirLazyPhaseResolver() {
    override fun resolve(
        designation: LLFirDesignationToResolve,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
        firProviderInterceptor: FirProviderInterceptor?
    ) {
        val resolver = LLFirTypeResolver(designation, lockProvider, session, scopeSession)
        resolver.resolve()
    }

    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(target, FirResolvePhase.TYPES, updateForLocalDeclarations = false)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(FirResolvePhase.TYPES)
        if (target is FirConstructor) {
            target.delegatedConstructor?.let { delegated ->
                checkTypeRefIsResolved(delegated.constructedTypeRef, "constructor type reference", target, acceptImplicitTypeRef = true)
            }
        }
        when (target) {
            is FirCallableDeclaration -> {
                checkReturnTypeRefIsResolved(target, acceptImplicitTypeRef = true)
                checkReceiverTypeRefIsResolved(target)
            }

            is FirTypeParameter -> {
                for (bound in target.bounds) {
                    checkTypeRefIsResolved(bound, "type parameter bound", target)
                }
            }

            else -> {}
        }
        checkNestedDeclarationsAreResolved(target)
        (target as? FirDeclaration)?.let { checkClassMembersAreResolved(it) }
    }
}


private class LLFirTypeResolver(
    designation: LLFirDesignationToResolve,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
) : LLFirAbstractMultiDesignationResolver(designation, lockProvider, FirResolvePhase.TYPES) {
    private val transformer = FirTypeResolveTransformer(session, scopeSession)

    private val containingClasses = mutableListOf<FirRegularClass>()

    override fun withFile(firFile: FirFile, action: () -> Unit) {
        transformer.withFileScope(firFile) {
            action()
        }
    }

    override fun withRegularClass(firClass: FirRegularClass, action: () -> Unit) {
        transformer.withClassDeclarationCleanup(firClass) {
            resolve(firClass) {
                resolveClassTypes(firClass)
            }

            transformer.withScopeCleanup {
                transformer.addTypeParametersScope(firClass)
                resolveContractors(firClass)
            }

            containingClasses += firClass
            try {
                transformer.withClassScopes(firClass) {
                    action()
                }
            } finally {
                containingClasses.removeLast()
            }
        }
    }

    private fun resolveContractors(firClass: FirRegularClass) {
        for (member in firClass.declarations) {
            if (member !is FirConstructor) continue
            resolve(member) {
                resolveConstructor(member, firClass)
            }
        }
    }

    private fun resolveConstructor(
        firConstructor: FirConstructor,
        firClass: FirRegularClass
    ) {
        // ConstructedTypeRef should be resolved only with type parameters, but not with nested classes and classes from supertypes
        transformer.transformDelegatedConstructorCall(firConstructor)

        transformer.withClassScopes(firClass) {
            firConstructor.accept(transformer, null)
        }

    }

    override fun resolveDeclarationContent(target: FirElementWithResolveState) {
        when (target) {
            is FirConstructor -> {
                // already resolved in withRegularClass
            }
            is FirDanglingModifierList, is FirFileAnnotationsContainer, is FirCallableDeclaration, is FirTypeAlias -> {
                target.accept(transformer, null)
            }

            is FirRegularClass -> {
                resolveClassTypes(target)
            }
            is FirAnonymousInitializer -> {}
            else -> error("Unknown declaration ${target::class.java}")
        }
    }

    private fun resolveClassTypes(firClass: FirRegularClass) {
        transformer.transformClassTypeParameters(firClass, null)
        transformer.withScopeCleanup {
            firClass.transformAnnotations(transformer, null)
        }

        transformer.withClassScopes(firClass) {
            for (contextReceiver in firClass.contextReceivers) {
                contextReceiver.transform<FirContextReceiver, _>(transformer, null)
            }
        }
    }
}