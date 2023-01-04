/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirDesignationForResolveWithMembers
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirDesignationForResolveWithMultipleTargets
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirDesignationToResolve
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase

internal abstract class LLFirAbstractMultiDesignationResolver(
    protected val designation: LLFirDesignationToResolve,
    protected val lockProvider: LLFirLockProvider,
    protected val phase: FirResolvePhase,
) {

    protected abstract fun withFile(firFile: FirFile, action: () -> Unit)

    protected abstract fun withRegularClass(firClass: FirRegularClass, action: () -> Unit)

    protected open fun withEnumEntry(enumEntry: FirEnumEntry, action: () -> Unit) {}
    protected open fun checkResolveConsistency() {}

    protected open fun resolveWithoutLock(target: FirElementWithResolveState): Boolean = false
    protected abstract fun resolveDeclarationContent(target: FirElementWithResolveState)

    fun resolve() {
        checkResolveConsistency()
        withFile(designation.firFile) {
            goToTargets(designation.path.iterator())
        }
    }

    private fun goToTargets(iterator: Iterator<FirRegularClass>) {
        if (iterator.hasNext()) {
            val firClass = iterator.next()
            withRegularClass(firClass) {
                goToTargets(iterator)
            }
        } else {
            when (designation) {
                is LLFirDesignationForResolveWithMembers -> {
                    resolveTarget(designation.target)
                    withRegularClass(designation.target) {
                        for (it in designation.callableMembersToResolve) {
                            resolveTarget(it)
                        }
                    }
                }
                is LLFirDesignationForResolveWithMultipleTargets -> {
                    for (target in designation.targets) {
                        resolveTargetWithPossiblyNestedDeclarations(target, designation.resolveMembersInsideTarget)
                    }
                }
            }
        }
    }

    private fun resolveTargetWithPossiblyNestedDeclarations(target: FirElementWithResolveState, resolveMembersInside: Boolean) {
        resolveTarget(target)
        if (resolveMembersInside) {
            when (target) {
                is FirRegularClass -> {
                    withRegularClass(target) {
                        for (member in target.declarations) {
                            resolveTargetWithPossiblyNestedDeclarations(member, resolveMembersInside = true)
                        }
                    }
                }
                is FirEnumEntry -> {
                    withEnumEntry(target) {

                    }
                }
            }
        }
    }

    protected fun resolveTarget(target: FirElementWithResolveState) {
        if (resolveWithoutLock(target)) return
        lockProvider.withLock(target, phase) {
            resolveDeclarationContent(target)
            LLFirLazyPhaseResolverByPhase.getByPhase(phase).updatePhaseForDeclarationInternals(target)
        }
    }

    protected inline fun resolve(target: FirElementWithResolveState, crossinline action: () -> Unit) {
        lockProvider.withLock(target, phase) {
            action()
            LLFirLazyPhaseResolverByPhase.getByPhase(phase).updatePhaseForDeclarationInternals(target)
        }
    }

}