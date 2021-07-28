/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirJvmStaticChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirCallableDeclaration) {
            return
        }

        if (!declaration.isOverride || !declaration.isContainerNotCompanionObject(context)) {
            return
        }

        if (declaration is FirProperty) {
            declaration.getter?.let {
                reportIfHasJvmStatic(it, context, reporter)
            }
            declaration.setter?.let {
                reportIfHasJvmStatic(it, context, reporter)
            }
        }

        reportIfHasJvmStatic(declaration, context, reporter)
    }

    private fun reportIfHasJvmStatic(declaration: FirAnnotatedDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.hasJvmStaticAnnotation()) {
            reporter.reportOn(declaration.source, FirJvmErrors.OVERRIDE_CANNOT_BE_STATIC, context)
        }
    }

    private fun FirDeclaration.isContainerNotCompanionObject(context: CheckerContext): Boolean {
        val containingClassSymbol = this.getContainingClassSymbol(context.session) ?: return false

        @OptIn(SymbolInternals::class)
        val containingClass = containingClassSymbol.fir.safeAs<FirRegularClass>() ?: return false

        return containingClass.classKind == ClassKind.OBJECT && !containingClass.isCompanion
    }

    private fun FirAnnotatedDeclaration.hasJvmStaticAnnotation(): Boolean {
        return findJvmStaticAnnotation() != null
    }

    private fun FirAnnotatedDeclaration.findJvmStaticAnnotation(): FirAnnotationCall? {
        return annotations.firstOrNull {
            it.calleeReference.safeAs<FirResolvedNamedReference>()?.name?.toString() == "JvmStatic"
        }
    }
}
