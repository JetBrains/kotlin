/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkAnnotationTypeIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkContextReceiverTypeRefIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkReceiverTypeRefIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkReturnTypeRefIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkTypeRefIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.transformers.FirTypeResolveTransformer
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal object LLFirTypeLazyResolver : LLFirLazyResolver(FirResolvePhase.TYPES) {
    override fun createTargetResolver(target: LLFirResolveTarget): LLFirTargetResolver = LLFirTypeTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: FirElementWithResolveState) {
        if (target is FirAnnotationContainer) {
            checkAnnotationTypeIsResolved(target)
        }

        if (target is FirConstructor) {
            target.delegatedConstructor?.let { delegated ->
                checkTypeRefIsResolved(delegated.constructedTypeRef, "constructor type reference", target, acceptImplicitTypeRef = true)
            }
        }

        when (target) {
            is FirCallableDeclaration -> {
                checkReturnTypeRefIsResolved(target, acceptImplicitTypeRef = true)
                checkReceiverTypeRefIsResolved(target)
                checkContextReceiverTypeRefIsResolved(target)
            }

            is FirTypeParameter -> {
                for (bound in target.bounds) {
                    checkTypeRefIsResolved(bound, "type parameter bound", target)
                }
            }
        }
    }
}

private class LLFirTypeTargetResolver(target: LLFirResolveTarget) : LLFirTargetResolver(target, FirResolvePhase.TYPES) {
    private val transformer = object : FirTypeResolveTransformer(resolveTargetSession, resolveTargetScopeSession) {
        override fun transformTypeRef(typeRef: FirTypeRef, data: Any?): FirResolvedTypeRef {
            FirLazyBodiesCalculator.calculateAnnotations(typeRef, session)
            return super.transformTypeRef(typeRef, data)
        }
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withFile", level = DeprecationLevel.ERROR)
    override fun withContainingFile(firFile: FirFile, action: () -> Unit) {
        transformer.withFileScope(firFile, action)
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withContainingRegularClass(firClass: FirRegularClass, action: () -> Unit) {
        firClass.lazyResolveToPhase(resolverPhase.previous)
        transformer.withClassDeclarationCleanup(firClass) {
            performCustomResolveUnderLock(firClass) {
                resolveClassTypes(firClass)
            }

            transformer.withClassScopes(
                firClass,
                action = action,
            )
        }
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        when (target) {
            is FirFunction -> resolve(target, TypeStateKeepers.FUNCTION)
            is FirProperty -> resolve(target, TypeStateKeepers.PROPERTY)
            is FirCallableDeclaration,
            is FirDanglingModifierList,
            is FirFileAnnotationsContainer,
            is FirTypeAlias,
            is FirScript,
            is FirRegularClass,
            is FirAnonymousInitializer,
            -> rawResolve(target)

            is FirFile, is FirCodeFragment -> {}
            else -> errorWithAttachment("Unknown declaration ${target::class.simpleName}") {
                withFirEntry("declaration", target)
            }
        }
    }

    private fun <T : FirElementWithResolveState> resolve(target: T, keeper: StateKeeper<T, Unit>) {
        resolveWithKeeper(target, Unit, keeper) {
            rawResolve(target)
        }
    }

    private fun rawResolve(target: FirElementWithResolveState) {
        when (target) {
            is FirConstructor -> {
                // ConstructedTypeRef should be resolved only with type parameters, but not with nested classes and classes from supertypes
                resolveOutsideClassBody(target, transformer::transformDelegatedConstructorCall)
            }
            is FirScript -> resolveScriptTypes(target)
            is FirDanglingModifierList, is FirFileAnnotationsContainer, is FirCallableDeclaration, is FirTypeAlias,
            is FirAnonymousInitializer,
            -> {
                if (target is FirField && target.origin == FirDeclarationOrigin.Synthetic.DelegateField) {
                    // delegated field should be resolved in the same context as super types
                    resolveOutsideClassBody(target, transformer::transformDelegateField)
                } else {
                    target.accept(transformer, null)
                }
            }

            is FirRegularClass -> resolveClassTypes(target)
            else -> errorWithAttachment("Unknown declaration ${target::class.simpleName}") {
                withFirEntry("declaration", target)
            }
        }
    }

    private inline fun <T : FirElementWithResolveState> resolveOutsideClassBody(
        target: T,
        crossinline actionOutsideClassBody: (T) -> Unit,
    ) {
        val scopesBeforeContainingClass = transformer.scopesBefore
            ?: errorWithFirSpecificEntries("The containing class scope is not found", fir = target)

        val staticScopesBeforeContainingClass = transformer.staticScopesBefore
            ?: errorWithFirSpecificEntries("The containing class static scope is not found", fir = target)

        @OptIn(PrivateForInline::class)
        transformer.withScopeCleanup {
            val clazz = transformer.classDeclarationsStack.last()
            if (!transformer.removeOuterTypeParameterScope(clazz)) {
                transformer.scopes = scopesBeforeContainingClass
            } else {
                transformer.scopes = staticScopesBeforeContainingClass
                transformer.addTypeParametersScope(clazz)
            }

            actionOutsideClassBody(target)
        }

        target.accept(transformer, null)
    }

    private fun resolveScriptTypes(firScript: FirScript) {
        firScript.annotations.forEach { it.accept(transformer, null) }
        firScript.contextReceivers.forEach { it.accept(transformer, null) }
    }

    private fun resolveClassTypes(firClass: FirRegularClass) {
        transformer.transformClassTypeParameters(firClass, null)
        transformer.withScopeCleanup {
            firClass.transformAnnotations(transformer, null)
        }

        transformer.withClassScopes(firClass) {
            for (contextReceiver in firClass.contextReceivers) {
                contextReceiver.transformSingle(transformer, null)
            }
        }
    }
}

private object TypeStateKeepers {
    val FUNCTION: StateKeeper<FirFunction, Unit> = stateKeeper { function, context ->
        add(CALLABLE_DECLARATION, context)
        entityList(function.valueParameters, CALLABLE_DECLARATION, context)
    }

    val PROPERTY: StateKeeper<FirProperty, Unit> = stateKeeper { property, context ->
        add(CALLABLE_DECLARATION, context)
        entity(property.getter, FUNCTION, context)
        entity(property.setter, FUNCTION, context)
        entity(property.backingField, CALLABLE_DECLARATION, context)
    }

    private val CALLABLE_DECLARATION: StateKeeper<FirCallableDeclaration, Unit> = stateKeeper { _, _ ->
        add(FirCallableDeclaration::returnTypeRef, FirCallableDeclaration::replaceReturnTypeRef)
    }
}
