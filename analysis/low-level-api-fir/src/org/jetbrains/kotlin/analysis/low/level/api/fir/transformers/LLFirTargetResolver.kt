/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirScript

internal abstract class LLFirTargetResolver(
    protected val resolveTarget: LLFirResolveTarget,
    protected val lockProvider: LLFirLockProvider,
    protected val resolverPhase: FirResolvePhase,
    private val isJumpingPhase: Boolean = false,
) : LLFirResolveTargetVisitor {
    private val _nestedClassesStack = mutableListOf<FirRegularClass>()

    val nestedClassesStack: List<FirRegularClass> get() = _nestedClassesStack.toList()

    /**
     * Must be executed without a lock
     */
    protected fun resolveFileAnnotationContainerIfNeeded(elementWithResolveState: FirElementWithResolveState) {
        if (elementWithResolveState !is FirFile) return
        val annotationContainer = elementWithResolveState.annotationsContainer ?: return
        withFile(elementWithResolveState) {
            performResolve(annotationContainer)
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
        _nestedClassesStack += firClass
        withRegularClassImpl(firClass, action)
        check(_nestedClassesStack.removeLast() === firClass)
    }

    protected open fun checkResolveConsistency() {}

    protected open fun doResolveWithoutLock(target: FirElementWithResolveState): Boolean {
        resolveFileAnnotationContainerIfNeeded(target)
        return false
    }

    protected abstract fun doLazyResolveUnderLock(target: FirElementWithResolveState)

    fun resolveDesignation() {
        checkResolveConsistency()
        resolveTarget.visit(this)
    }

    final override fun performAction(element: FirElementWithResolveState) {
        performResolve(element)
    }

    protected fun performResolve(target: FirElementWithResolveState) {
        if (doResolveWithoutLock(target)) return
        performCustomResolveUnderLock(target) {
            doLazyResolveUnderLock(target)
        }
    }

    protected inline fun performCustomResolveUnderLock(target: FirElementWithResolveState, crossinline action: () -> Unit) {
        checkThatResolvedAtLeastToPreviousPhase(target)
        withPossiblyJumpingLock(target) {
            action()
            LLFirLazyPhaseResolverByPhase.getByPhase(resolverPhase).updatePhaseForDeclarationInternals(target)
        }
    }

    private inline fun withPossiblyJumpingLock(target: FirElementWithResolveState, action: () -> Unit) {
        if (isJumpingPhase) {
            lockProvider.withJumpingLock(target, resolverPhase, action)
        } else {
            lockProvider.withWriteLock(target, resolverPhase, action)
        }
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
