/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirJvmStaticChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirConstructor) {
            // WRONG_DECLARATION_TARGET
            return
        }

        checkOverrideCannotBeStatic(declaration, context, reporter)
        checkStaticNotInProperObject(declaration, context, reporter)
    }

    private fun checkStaticNotInProperObject(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirCallableDeclaration) {
            return
        }

        val containingClassSymbol = declaration.getContainingClassSymbol(context.session) ?: return
        val supportJvmStaticInInterface = context.session.languageVersionSettings.supportsFeature(LanguageFeature.JvmStaticInInterface)

        val properDiagnostic = if (supportJvmStaticInInterface) {
            FirJvmErrors.JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION
        } else {
            FirJvmErrors.JVM_STATIC_NOT_IN_OBJECT_OR_CLASS_COMPANION
        }

        var shouldReport = false

        if (containingClassSymbol.classKind != ClassKind.OBJECT) {
            shouldReport = true
        } else {
            val declaringClassSymbol = containingClassSymbol.getContainingClassSymbol(context.session) ?: return

            if (declaringClassSymbol.classKind?.isInterface == true && !supportJvmStaticInInterface) {
                shouldReport = true
            }
        }

        if (!shouldReport) {
            return
        }

        declaration.reportOnProperParts {
            if (it.hasAnnotationNamedAs(StandardClassIds.JvmStatic)) {
                reporter.reportOn(it.source, properDiagnostic, context)
            }
        }
    }

    private fun checkOverrideCannotBeStatic(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirCallableDeclaration) {
            return
        }

        if (!declaration.isOverride || !declaration.isContainerNonCompanionObject(context)) {
            return
        }

        declaration.reportOnProperParts {
            if (it.hasAnnotationNamedAs(StandardClassIds.JvmStatic)) {
                reporter.reportOn(it.source, FirJvmErrors.OVERRIDE_CANNOT_BE_STATIC, context)
            }
        }
    }

    private fun FirAnnotatedDeclaration.reportOnProperParts(report: (FirAnnotatedDeclaration) -> Unit) {
        if (this is FirProperty) {
            // the setter is visited separately
            this.getter?.let(report)
        }
        report(this)
    }

    private fun FirDeclaration.isContainerNonCompanionObject(context: CheckerContext): Boolean {
        val containingClassSymbol = this.getContainingClassSymbol(context.session) ?: return false

        @OptIn(SymbolInternals::class)
        val containingClass = containingClassSymbol.fir.safeAs<FirRegularClass>() ?: return false

        return containingClass.classKind == ClassKind.OBJECT && !containingClass.isCompanion
    }

    private fun FirAnnotatedDeclaration.hasAnnotationNamedAs(classId: ClassId): Boolean {
        return findAnnotation(classId.shortClassName) != null
    }

    private fun FirAnnotatedDeclaration.findAnnotation(name: Name): FirAnnotationCall? {
        return annotations.firstOrNull {
            it.calleeReference.safeAs<FirResolvedNamedReference>()?.name == name
        }
    }
}
