/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.forEachDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isDeclarationContainer
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirScript

internal abstract class LLFirTargetResolver(
    protected val resolveTarget: LLFirResolveTarget,
    protected val lockProvider: LLFirLockProvider,
    protected val resolverPhase: FirResolvePhase,
    private val isJumpingPhase: Boolean = false,
) {
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

    protected open fun withFile(firFile: FirFile, action: () -> Unit) {
        action()
    }

    protected open fun withScript(firScript: FirScript, action: () -> Unit) {
        action()
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    protected open fun withRegularClassImpl(firClass: FirRegularClass, action: () -> Unit) {
        action()
    }

    @Suppress("DEPRECATION_ERROR")
    protected fun withRegularClass(firClass: FirRegularClass, action: () -> Unit) {
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
        if (resolveTarget is LLFirSingleResolveTarget && resolveTarget.target is FirFile) {
            performResolve(resolveTarget.firFile)
            return
        }
        if (resolveTarget is LLFirWholeFileResolveTarget) {
            performResolve(resolveTarget.firFile)
        }
        withFile(resolveTarget.firFile) {
            goToTargets(resolveTarget.path.iterator())
        }
    }

    private fun goToTargets(path: Iterator<FirDeclaration>) {
        if (path.hasNext()) {
            when (val firDeclaration = path.next()) {
                is FirRegularClass -> withRegularClass(firDeclaration) { goToTargets(path) }
                is FirScript -> withScript(firDeclaration) { goToTargets(path) }
                else -> errorWithFirSpecificEntries(
                    "Unexpected declaration in path: ${firDeclaration::class.simpleName}",
                    fir = firDeclaration,
                )
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
                is LLFirWholeClassResolveTarget -> {
                    performResolve(resolveTarget.target)
                    withRegularClass(resolveTarget.target) {
                        resolveTarget.forEachDeclaration(::resolveTargetWithNestedDeclarations)
                    }
                }
            }
        }
    }

    private fun resolveTargetWithNestedDeclarations(target: FirElementWithResolveState) {
        performResolve(target)
        when {
            target !is FirDeclaration || !target.isDeclarationContainer -> {}

            target is FirRegularClass -> withRegularClass(target) {
                target.forEachDeclaration(::resolveTargetWithNestedDeclarations)
            }

            target is FirScript -> withScript(target) {
                target.forEachDeclaration(::resolveTargetWithNestedDeclarations)
            }

            else -> errorWithFirSpecificEntries("Unexpected declaration: ${target::class.simpleName}", fir = target)
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
