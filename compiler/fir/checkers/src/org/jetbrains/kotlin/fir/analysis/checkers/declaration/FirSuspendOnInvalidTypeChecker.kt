/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRefWithNullability
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.lexer.KtTokens

object FirSuspendOnInvalidTypeChecker : FirFileChecker() {
    override fun check(declaration: FirFile, context: CheckerContext, reporter: DiagnosticReporter) {
        // Traverse the entire file to look for source type refs (i.e., Fir(Dynamic|User|Function)TypeRef).
        declaration.accept(object : FirDefaultVisitorVoid() {
            override fun visitElement(element: FirElement) {
                if (element.source?.kind is FirFakeSourceElementKind) return
                element.acceptChildren(this)
            }

            override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
                // We will only get to the source type refs by visiting delegatedTypeRef.
                super.visitResolvedTypeRef(resolvedTypeRef)
                resolvedTypeRef.delegatedTypeRef?.accept(this)
            }

            override fun visitTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability) {
                // `suspend` is invalid for non-function types (i.e., FirDynamicTypeRef or FirUserTypeRef).
                //
                // It is also invalid for nullable function types, e.g., `suspend (() -> Int)?`.
                // The correct way to denote a nullable suspend function type is `(suspend () -> Int)?`.
                // In both cases, the source for the FirFunctionTypeRef is the TYPE_REFERENCE element.
                // In the invalid case, the modifier list is in the source TYPE_REFERENCE element.
                // But in the correct case, the modifier list is in the child NULLABLE_TYPE element.
                // Therefore, if the FirFunctionTypeRef is marked nullable, it is invalid to HAVE the `suspend` modifier
                // on the source element, even though it seems counter-intuitive.
                val suspendModifier = typeRefWithNullability.getModifier(KtTokens.SUSPEND_KEYWORD) ?: return
                if (typeRefWithNullability !is FirFunctionTypeRef || typeRefWithNullability.isMarkedNullable) {
                    reporter.reportOn(
                        suspendModifier.source,
                        FirErrors.WRONG_MODIFIER_TARGET,
                        suspendModifier.token,
                        "non-functional type",
                        context
                    )
                }
            }
        })
    }
}