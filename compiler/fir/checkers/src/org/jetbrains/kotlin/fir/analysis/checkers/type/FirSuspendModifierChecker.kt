/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.forEachChildOfType
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRefWithNullability
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

object FirSuspendModifierChecker : FirTypeRefChecker(MppCheckerKind.Common) {
    private val suspendTokenElementSet = setOf(KtTokens.SUSPEND_KEYWORD)

    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        // We are only interested in source type refs (i.e., Fir(Dynamic|User|Function)TypeRef).
        if (typeRef !is FirTypeRefWithNullability) return

        val suspendModifierSources = mutableListOf<KtSourceElement>()
        typeRef.source.getChild(KtStubElementTypes.MODIFIER_LIST, depth = 1)?.forEachChildOfType(suspendTokenElementSet, depth = 1) {
            suspendModifierSources += it
        }
        if (suspendModifierSources.isEmpty()) return

        // `suspend` is invalid for non-function types (i.e., FirDynamicTypeRef or FirUserTypeRef).
        //
        // It is also invalid for nullable function types, e.g., `suspend (() -> Int)?`.
        // The correct way to denote a nullable suspend function type is `(suspend () -> Int)?`.
        // (To clarify: You can "mark nullable" a suspend function type, but you cannot "mark suspend" a nullable function type.)
        //
        // In both invalid and correct cases, the source for the FirFunctionTypeRef is the TYPE_REFERENCE element.
        // In the invalid case, the `suspend` modifier is in the source TYPE_REFERENCE element.
        // But in the correct case, the `suspend` modifier is in the child NULLABLE_TYPE element, i.e., the source TYPE_REFERENCE element
        // will not have the `suspend` modifier.
        //
        // In both cases, the FirFunctionTypeRef is marked nullable. But it is invalid to have the `suspend` modifier on the source element.
        if (typeRef !is FirFunctionTypeRef || typeRef.isMarkedNullable) {
            reporter.reportOn(
                suspendModifierSources.first(),
                FirErrors.WRONG_MODIFIER_TARGET,
                KtTokens.SUSPEND_KEYWORD,
                "non-functional type",
                context
            )
        } else if (suspendModifierSources.size > 1) {
            reporter.reportOn(
                suspendModifierSources.first(),
                FirErrors.REPEATED_MODIFIER,
                KtTokens.SUSPEND_KEYWORD,
                context,
            )
        }
    }
}
