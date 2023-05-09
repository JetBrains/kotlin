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

internal abstract class LLFirTargetResolver(
    protected val resolveTarget: LLFirResolveTarget,
    protected val lockProvider: LLFirLockProvider,
    protected val resolverPhase: FirResolvePhase,
    private val isJumpingPhase: Boolean = false,
) {
    private val _nestedClassesStack = mutableListOf<FirRegularClass>()

    val nestedClassesStack: List<FirRegularClass> get() = _nestedClassesStack.toList()

    protected abstract fun withFile(firFile: FirFile, action: () -> Unit)

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    protected abstract fun withRegularClassImpl(firClass: FirRegularClass, action: () -> Unit)

    @Suppress("DEPRECATION_ERROR")
    protected fun withRegularClass(firClass: FirRegularClass, action: () -> Unit) {
        _nestedClassesStack += firClass
        withRegularClassImpl(firClass, action)
        check(_nestedClassesStack.removeLast() === firClass)
    }

    protected open fun checkResolveConsistency() {}

    protected open fun doResolveWithoutLock(target: FirElementWithResolveState): Boolean = false
    protected abstract fun doLazyResolveUnderLock(target: FirElementWithResolveState)

    fun resolveDesignation() {
        checkResolveConsistency()
        withFile(resolveTarget.firFile) {
            goToTargets(resolveTarget.path.iterator())
        }
    }

    private fun goToTargets(iterator: Iterator<FirRegularClass>) {
        if (iterator.hasNext()) {
            val firClass = iterator.next()
            withRegularClass(firClass) {
                goToTargets(iterator)
            }
        } else {
            when (resolveTarget) {
                is LLFirClassWithSpecificMembersResolveTarget -> {
                    performResolve(resolveTarget.target)
                    withRegularClass(resolveTarget.target) {
                        resolveTarget.forEachMember(::performResolve)
                    }
                }
                is LLFirClassWithAllMembersResolveTarget -> {
                    performResolve(resolveTarget.target)
                    withRegularClass(resolveTarget.target) {
                        resolveTarget.forEachMember(::performResolve)
                    }
                }
                is LLFirClassWithAllCallablesResolveTarget -> {
                    performResolve(resolveTarget.target)
                    withRegularClass(resolveTarget.target) {
                        resolveTarget.forEachCallable(::performResolve)
                    }
                }
                is LLFirSingleResolveTarget -> {
                    performResolve(resolveTarget.target)
                }
                is LLFirWholeFileResolveTarget -> {
                    resolveTarget.forEachTopLevelDeclaration(::resolveTargetWithNestedDeclarations)
                }
            }
        }
    }

    private fun resolveTargetWithNestedDeclarations(target: FirElementWithResolveState) {
        performResolve(target)
        if (target is FirRegularClass) {
            withRegularClass(target) {
                for (member in target.declarations) {
                    resolveTargetWithNestedDeclarations(member)
                }
            }
        }
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