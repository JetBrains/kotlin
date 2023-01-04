/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*

internal interface LLFirLazyTransformer {
    fun transformDeclaration(phaseRunner: LLFirPhaseRunner)

    fun checkIsResolved(target: FirElementWithResolveState)

    fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState)

    fun checkNestedDeclarationsAreResolved(target: FirElementWithResolveState) {
        if (target !is FirDeclaration) return
        checkFunctionParametersAreResolved(target)
        checkPropertyAccessorsAreResolved(target)
        checkClassMembersAreResolved(target)
    }

    fun checkClassMembersAreResolved(declaration: FirDeclaration) {
        if (!needCheckingIfClassMembersAreResolved) return
        if (declaration is FirClass) {
            for (member in declaration.declarations) {
                checkClassMembersAreResolved(member)
            }
        }
    }

    fun checkPropertyAccessorsAreResolved(declaration: FirDeclaration) {
        if (declaration is FirProperty) {
            declaration.getter?.let { checkIsResolved(it) }
            declaration.setter?.let { checkIsResolved(it) }
        }
    }


    fun checkFunctionParametersAreResolved(declaration: FirDeclaration) {
        if (declaration is FirFunction) {
            for (parameter in declaration.valueParameters) {
                checkIsResolved(parameter)
            }
        }
    }

    fun checkTypeParametersAreResolved(declaration: FirDeclaration) {
        if (declaration is FirTypeParameterRefsOwner) {
            for (parameter in declaration.typeParameters) {
                if (parameter is FirTypeParameter) {
                    checkIsResolved(parameter)
                }
            }
        }
    }

    companion object {
        internal var needCheckingIfClassMembersAreResolved: Boolean = false
            @TestOnly set

        val DUMMY = object : LLFirLazyTransformer {
            override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) {}
            override fun checkIsResolved(target: FirElementWithResolveState) {}
            override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {}
        }
    }
}
