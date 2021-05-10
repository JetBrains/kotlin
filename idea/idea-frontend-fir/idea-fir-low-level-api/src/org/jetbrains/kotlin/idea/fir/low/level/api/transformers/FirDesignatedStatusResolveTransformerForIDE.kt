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
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationUntypedDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.util.targetContainingDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ensurePathPhase
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ensureTargetPhase

internal class FirDesignatedStatusResolveTransformerForIDE(
    private val designation: FirDeclarationUntypedDesignationWithFile,
    private val session: FirSession,
    private val scopeSession: ScopeSession,
) : FirLazyTransformerForIDE {

    private fun resolveTopLevelDeclaration(declaration: FirDeclaration) {
        val transformer = FirStatusResolveTransformer(
            session = session,
            scopeSession = scopeSession,
            statusComputationSession = StatusComputationSession.Regular()
        )
        declaration.transformSingle(transformer, null)
    }

    private fun resolveClassMember(containingClass: FirClass<*>, targetDeclaration: FirDeclaration) {

        val transformer = object : FirDesignatedStatusResolveTransformer(
            session = session,
            scopeSession = scopeSession,
            designation = designation.toSequence(includeTarget = true).iterator(),
            targetClass = if (targetDeclaration is FirRegularClass) targetDeclaration else containingClass,
            statusComputationSession = StatusComputationSession.Regular(),
            designationMapForLocalClasses = emptyMap(),
            scopeForLocalClass = null
        ) {

            override fun <F : FirClass<F>> transformClass(klass: FirClass<F>, data: FirResolvedDeclarationStatus?): FirStatement {
                if (klass != containingClass) return super.transformClass(klass, data)
                val result = storeClass(klass) {
                    targetDeclaration.transformSingle(this, data)
                }
                return result as FirStatement
            }
        }

        val firstItemInDesignation = designation.path.firstOrNull() ?: designation.declaration
        firstItemInDesignation.transformSingle(transformer, null)
    }

    override fun transformDeclaration() {
        if (designation.declaration.resolvePhase >= FirResolvePhase.STATUS) return
        designation.ensurePathPhase(FirResolvePhase.TYPES)
        designation.ensureTargetPhase(FirResolvePhase.TYPES)

        val containingClass = designation.targetContainingDeclaration()
        if (containingClass == null) {
            resolveTopLevelDeclaration(designation.declaration)
        } else {
            check(containingClass is FirClass<*>) { "Invalid designation - the parent is not a class" }
            resolveClassMember(containingClass, designation.declaration)
        }

        designation.ensureTargetPhase(FirResolvePhase.STATUS)
    }
}