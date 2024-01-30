/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.componentFunctionSymbol
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.originalIfFakeOverrideOrDelegated
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

internal abstract class LLFirTargetResolver(
    protected val resolveTarget: LLFirResolveTarget,
    protected val lockProvider: LLFirLockProvider,
    protected val resolverPhase: FirResolvePhase,
    private val isJumpingPhase: Boolean = false,
) : LLFirResolveTargetVisitor {
    val resolveTargetSession: LLFirSession get() = resolveTarget.session

    private val _nestedClassesStack = mutableListOf<FirRegularClass>()

    val nestedClassesStack: List<FirRegularClass> get() = _nestedClassesStack

    /**
     * @param context used as a context in the case of exception
     * @return the last class from [nestedClassesStack]
     */
    fun containingClass(context: FirElement): FirRegularClass = nestedClassesStack.lastOrNull()
        ?: errorWithAttachment("Containing class is not found") {
            withFirEntry("context", context)
        }

    protected inline fun withClassInStack(clazz: FirRegularClass, action: () -> Unit) {
        _nestedClassesStack += clazz
        action()
        val removed = _nestedClassesStack.removeLast()
        checkWithAttachment(removed === clazz, { "Unexpected state"}) {
            withFirEntry("expected", clazz)
            withFirEntry("actual", removed)
        }
    }

    /**
     * @see resolveDependencies
     */
    open val skipDependencyTargetResolutionStep: Boolean get() = false

    /**
     * Requests the resolution for dependencies to avoid race in the case of FIR instance sharing.
     * Will be executed before resolution without a lock.
     *
     * @see skipDependencyTargetResolutionStep
     */
    private fun resolveDependencies(target: FirElementWithResolveState) {
        if (skipDependencyTargetResolutionStep || target is FirFileAnnotationsContainer) return
        resolveTarget.firFile?.annotationsContainer?.lazyResolveToPhase(resolverPhase)

        val originalDeclaration = (target as? FirCallableDeclaration)?.originalIfFakeOverrideOrDelegated()
        when {
            // Fake or delegate declaration shared types and annotations from the original one
            originalDeclaration != null -> originalDeclaration.lazyResolveToPhase(resolverPhase)

            // We share type references and annotations with the original parameter
            target is FirProperty -> target.correspondingValueParameterFromPrimaryConstructor?.lazyResolveToPhase(resolverPhase)
            target is FirSimpleFunction && target.origin == FirDeclarationOrigin.Synthetic.DataClassMember -> {
                resolveDataClassMemberDependencies(target)
            }

            // delegate field shares the return type with the containing class
            // constructor shares types inside delegation call with the containing class
            target is FirField && target.origin == FirDeclarationOrigin.Synthetic.DelegateField || target is FirConstructor -> {
                containingClass(target).lazyResolveToPhase(resolverPhase)
            }
        }
    }

    private fun resolveDataClassMemberDependencies(function: FirSimpleFunction) {
        when {
            /**
             * componentN method shares the return type with the corresponding property
             * also status for the componentN method transforms during the corresponding property transformation
             * [org.jetbrains.kotlin.fir.resolve.transformers.AbstractFirStatusResolveTransformer.transformProperty]
             */
            DataClassResolver.isComponentLike(function.name) -> {
                val property = containingClass(function).declarations.firstNotNullOfOrNull { declaration ->
                    (declaration as? FirProperty)?.takeIf { it.componentFunctionSymbol?.fir == function }
                }

                property?.lazyResolveToPhase(resolverPhase)
            }

            // copy method shares the return type of generated properties as the return type
            // of corresponding value parameter and annotations from them
            DataClassResolver.isCopy(function.name) -> {
                for (declaration in containingClass(function).declarations) {
                    val property = declaration as? FirProperty ?: continue
                    if (property.fromPrimaryConstructor == true) {
                        property.lazyResolveToPhase(resolverPhase)
                    } else {
                        break // all generated properties are sequential, so we can stop if we encounter another property
                    }
                }
            }
        }
    }

    override fun withFile(firFile: FirFile, action: () -> Unit) {
        action()
    }

    override fun withScript(firScript: FirScript, action: () -> Unit) {
        action()
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    protected open fun withRegularClassImpl(firClass: FirRegularClass, action: () -> Unit) {
        action()
    }

    @Suppress("DEPRECATION_ERROR")
    final override fun withRegularClass(firClass: FirRegularClass, action: () -> Unit) {
        withClassInStack(firClass) {
            withRegularClassImpl(firClass, action)
        }
    }

    protected open fun checkResolveConsistency() {}

    protected open fun doResolveWithoutLock(target: FirElementWithResolveState): Boolean = false

    protected abstract fun doLazyResolveUnderLock(target: FirElementWithResolveState)

    fun resolveDesignation() {
        checkResolveConsistency()
        resolveTarget.visit(this)
    }

    final override fun performAction(element: FirElementWithResolveState) {
        performResolve(element)
    }

    protected fun performResolve(target: FirElementWithResolveState) {
        resolveDependencies(target)

        if (doResolveWithoutLock(target)) return

        if (isJumpingPhase) {
            lockProvider.withJumpingLock(
                target,
                resolverPhase,
                actionUnderLock = {
                    doLazyResolveUnderLock(target)
                    updatePhaseForDeclarationInternals(target)
                },
                actionOnCycle = {
                    handleCycleInResolution(target)
                }
            )
        } else {
            performCustomResolveUnderLock(target) {
                doLazyResolveUnderLock(target)
            }
        }
    }

    /**
     * Will be executed in the case of detected cycle between elements during jumping resolve.
     *
     * **There is no guaranties that [target] is guarded by the lock of the current thread**
     *
     * @param target an element with detected cycle
     *
     * @see LLFirLockProvider.withJumpingLock
     */
    protected open fun handleCycleInResolution(target: FirElementWithResolveState) {
        errorWithFirSpecificEntries("Resolution cycle is detected", fir = target)
    }

    /**
     * Execute [action] under the write lock in the context of [target].
     *
     * Allowed only for non-jumping phases.
     *
     * @see isJumpingPhase
     */
    protected inline fun performCustomResolveUnderLock(target: FirElementWithResolveState, crossinline action: () -> Unit) {
        checkThatResolvedAtLeastToPreviousPhase(target)
        requireWithAttachment(!isJumpingPhase, { "This function cannot be called for jumping phase" }) {
            withFirEntry("target", target)
        }

        lockProvider.withWriteLock(target, resolverPhase) {
            action()
            updatePhaseForDeclarationInternals(target)
        }
    }

    private fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirLazyPhaseResolverByPhase.getByPhase(resolverPhase).updatePhaseForDeclarationInternals(target)
    }

    /**
     * Execute action under a declaration lock.
     * [action] will be executed only once in case of successful lock.
     * If some another thread is already resolved [target] declaration to [resolverPhase] then [action] won't be executed.
     */
    protected inline fun withReadLock(target: FirElementWithResolveState, action: () -> Unit) {
        checkThatResolvedAtLeastToPreviousPhase(target)
        lockProvider.withReadLock(target, resolverPhase, action)
    }

    private fun checkThatResolvedAtLeastToPreviousPhase(target: FirElementWithResolveState) {
        when (val previousPhase = resolverPhase.previous) {
            FirResolvePhase.IMPORTS -> {}
            else -> {
                target.checkPhase(previousPhase)
            }
        }
    }
}
