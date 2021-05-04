/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirDesignatedStatusResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirStatusResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.StatusComputationSession
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignation
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.ensurePhase

class FirDesignatedStatusResolveTransformerForIDE(
    private val designation: FirDeclarationDesignation,
    private val session: FirSession,
    private val scopeSession: ScopeSession,
): FirLazyTransformerForIDE {

    private fun resolveClass(targetClass: FirClass<*>) {
        val transformer = FirDesignatedStatusResolveTransformer(
            session = session,
            scopeSession = scopeSession,
            designation = designation.fullDesignation.iterator(),
            targetClass = targetClass,
            statusComputationSession = StatusComputationSession.Regular(),
            designationMapForLocalClasses = emptyMap(),
            scopeForLocalClass = null)

        val firstClass = designation.fullDesignation.first()
        firstClass.transformSingle(transformer, null)
    }

    private fun resolveTopLevelMethod(targetCallable: FirCallableDeclaration<*>) {
        val transformer = FirStatusResolveTransformer(
            session = session,
            scopeSession = scopeSession,
            statusComputationSession = StatusComputationSession.Regular()
        )
        targetCallable.transformSingle(transformer, null)
    }

    private fun resolveClassMember(containingClass: FirClass<*>, targetCallable: FirCallableDeclaration<*>) {

        val transformer = object : FirDesignatedStatusResolveTransformer(
            session = session,
            scopeSession = scopeSession,
            designation = designation.fullDesignation.iterator(),
            targetClass = containingClass,
            statusComputationSession = StatusComputationSession.Regular(),
            designationMapForLocalClasses = emptyMap(),
            scopeForLocalClass = null) {

            override fun <F : FirClass<F>> transformClass(
                klass: FirClass<F>,
                data: FirResolvedDeclarationStatus?
            ): FirStatement {
                return storeClass(klass) {
                    targetCallable.transformSingle(this, data)
                } as FirStatement
            }
        }

        val firstClass = designation.fullDesignation.first()
        firstClass.transformSingle(transformer, null)
    }

    override fun transformDeclaration() {
        designation.ensurePhase(FirResolvePhase.STATUS, exceptLast = true)
        when(val resolveTarget = designation.declaration) {
            is FirClass<*> -> resolveClass(resolveTarget)
            is FirCallableDeclaration<*> -> {
                val containingClass = designation.path.lastOrNull()
                if (containingClass == null) {
                    check(designation.fullDesignation.size == 1) { "Invalid designation - should be single element designation for top level declaration" }
                    resolveTopLevelMethod(resolveTarget)
                } else {
                    check(containingClass is FirClass<*>) { "Invalid designation - the parent of callable is not a class" }
                    resolveClassMember(containingClass, resolveTarget)
                }
            }
            else -> error("Declaration should be ${FirClass::class.simpleName} but given ${resolveTarget::class.simpleName}")
        }
    }
}