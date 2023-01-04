/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirDesignationForResolveWithMembers
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirDesignationForResolveWithMultipleTargets
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirDesignationToResolve
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector

internal abstract class LLFirLazyPhaseResolver {
    abstract fun resolve(
        designation: LLFirDesignationToResolve,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
        firProviderInterceptor: FirProviderInterceptor?,
    )

    abstract fun checkIsResolved(target: FirElementWithResolveState)

    fun checkIsResolved(designation: LLFirDesignationToResolve) {
        when (designation) {
            is LLFirDesignationForResolveWithMembers -> {
                checkIsResolved(designation.target)
                for (member in designation.callableMembersToResolve) {
                    checkIsResolved(member)
                }
            }
            is LLFirDesignationForResolveWithMultipleTargets -> {
                for (target in designation.targets) {
                    checkIsResolved(target)
                    if (designation.resolveMembersInsideTarget) {
                        checkClassMembersAreResolved(target)
                    }
                }
            }
        }
    }

    abstract fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState)

    protected fun checkNestedDeclarationsAreResolved(target: FirElementWithResolveState) {
        if (target !is FirDeclaration) return
        checkFunctionParametersAreResolved(target)
        checkPropertyAccessorsAreResolved(target)
        checkTypeParametersAreResolved(target)
    }

    protected fun checkClassMembersAreResolved(target: FirElementWithResolveState) {
        if (target is FirClass) {
            for (member in target.declarations) {
                checkClassMembersAreResolved(member)
            }
        }
    }

    private fun checkPropertyAccessorsAreResolved(declaration: FirDeclaration) {
        if (declaration is FirProperty) {
            declaration.getter?.let { checkIsResolved(it) }
            declaration.setter?.let { checkIsResolved(it) }
        }
    }


    private fun checkFunctionParametersAreResolved(declaration: FirDeclaration) {
        if (declaration is FirFunction) {
            for (parameter in declaration.valueParameters) {
                checkIsResolved(parameter)
            }
        }
    }

    private fun checkTypeParametersAreResolved(declaration: FirDeclaration) {
        if (declaration is FirTypeParameterRefsOwner) {
            for (parameter in declaration.typeParameters) {
                if (parameter is FirTypeParameter) {
                    checkIsResolved(parameter)
                }
            }
        }
    }
}
