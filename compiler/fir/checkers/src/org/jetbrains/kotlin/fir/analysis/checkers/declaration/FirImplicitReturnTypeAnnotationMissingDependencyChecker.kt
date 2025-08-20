/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.typeAnnotations

object FirImplicitReturnTypeAnnotationMissingDependencyChecker : FirCallableDeclarationChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirCallableDeclaration) {
        if (declaration.isLocalMember) return
        val returnTypeRef = declaration.returnTypeRef
        val source = returnTypeRef.source
        if (source?.kind != KtFakeSourceElementKind.ImplicitTypeRef) return

        check(returnTypeRef, source)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun check(returnTypeRef: FirTypeRef, source: KtSourceElement?) {
        returnTypeRef.annotations.forEach {
            check(it, source)
        }

        returnTypeRef.coneType.typeAnnotations.forEach {
            check(it, source)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun check(it: FirAnnotation, source: AbstractKtSourceElement?) {
        val coneType = it.annotationTypeRef.coneType
        if (coneType.toSymbol(context.session) == null) {
            reporter.reportOn(source, FirErrors.MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION, coneType)
        }
    }
}