/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
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

        if (declaration !is FirAnnotatedDeclaration) {
            return
        }

        if (declaration !is FirMemberDeclaration) {
            return
        }

        val annotatedParts = declaration.getAnnotatedParts()

        checkOverrideCannotBeStatic(declaration, context, reporter, annotatedParts)
        checkStaticNotInProperObject(context, reporter, annotatedParts)
        checkStaticNonPublicOrExternal(declaration, context, reporter, annotatedParts)
        checkStaticOnConstOrJvmField(context, reporter, annotatedParts)
    }

    private fun checkStaticOnConstOrJvmField(
        context: CheckerContext,
        reporter: DiagnosticReporter,
        annotatedParts: List<FirAnnotatedDeclaration>,
    ) {
        annotatedParts.forEach {
            if (
                it is FirProperty && it.isConst ||
                it.hasAnnotationNamedAs(StandardClassIds.JvmField)
            ) {
                reporter.reportOn(it.source, FirJvmErrors.JVM_STATIC_ON_CONST_OR_JVM_FIELD, context)
            }
        }
    }

    private fun checkStaticNonPublicOrExternal(
        declaration: FirMemberDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        annotatedParts: List<FirAnnotatedDeclaration>,
    ) {
        val containingClassSymbol = context.getContainerAt(0) ?: return
        var shouldCheck = false

        if (containingClassSymbol.classKind != ClassKind.OBJECT) {
            shouldCheck = true
        } else {
            if (context.containerIsInterface(1)) {
                shouldCheck = true
            }
        }

        if (!shouldCheck) {
            return
        }

        val minVisibility = declaration.getMinimumVisibility()

        if (minVisibility != Visibilities.Public) {
            annotatedParts.forEach {
                reporter.reportOn(it.source, FirJvmErrors.JVM_STATIC_ON_NON_PUBLIC_MEMBER, context)
            }
        } else if (declaration.isExternal) {
            annotatedParts.forEach {
                reporter.reportOn(it.source, FirJvmErrors.JVM_STATIC_ON_EXTERNAL_IN_INTERFACE, context)
            }
        }
    }

    private fun FirMemberDeclaration.getMinimumVisibility() = when (this) {
        is FirProperty -> getMinimumVisibility()
        else -> this.visibility
    }

    private fun FirProperty.getMinimumVisibility(): Visibility {
        var minVisibility = visibility

        getter?.let {
            minVisibility = chooseMostSpecific(minVisibility, it.visibility)
        }

        setter?.let {
            minVisibility = chooseMostSpecific(minVisibility, it.visibility)
        }

        return minVisibility
    }

    private fun chooseMostSpecific(a: Visibility, b: Visibility): Visibility {
        val difference = a.compareTo(b) ?: return a
        return if (difference > 0) {
            b
        } else {
            a
        }
    }

    private fun checkStaticNotInProperObject(
        context: CheckerContext,
        reporter: DiagnosticReporter,
        annotatedParts: List<FirAnnotatedDeclaration>,
    ) {
        val containingClassSymbol = context.getContainerAt(0) ?: return
        val supportJvmStaticInInterface = context.session.languageVersionSettings.supportsFeature(LanguageFeature.JvmStaticInInterface)

        val properDiagnostic = if (supportJvmStaticInInterface) {
            FirJvmErrors.JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION
        } else {
            FirJvmErrors.JVM_STATIC_NOT_IN_OBJECT_OR_CLASS_COMPANION
        }

        var shouldReport = false

        if (containingClassSymbol.classKind != ClassKind.OBJECT) {
            shouldReport = true
        } else if (
            containingClassSymbol is FirRegularClassSymbol &&
            containingClassSymbol.isCompanion &&
            context.containerIsInterface(1) &&
            !supportJvmStaticInInterface
        ) {
            shouldReport = true
        }

        if (!shouldReport) {
            return
        }

        annotatedParts.forEach {
            reporter.reportOn(it.source, properDiagnostic, context)
        }
    }

    private fun checkOverrideCannotBeStatic(
        declaration: FirMemberDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        annotatedParts: List<FirAnnotatedDeclaration>,
    ) {
        if (!declaration.isOverride || !context.containerIsNonCompanionObject(0)) {
            return
        }

        annotatedParts.forEach {
            reporter.reportOn(it.source, FirJvmErrors.OVERRIDE_CANNOT_BE_STATIC, context)
        }
    }

    private fun FirAnnotatedDeclaration.getAnnotatedParts(): List<FirAnnotatedDeclaration> {
        return getReportableParts().filter {
            it.hasAnnotationNamedAs(StandardClassIds.JvmStatic)
        }
    }

    private fun FirAnnotatedDeclaration.getReportableParts(): List<FirAnnotatedDeclaration> {
        val parts = mutableListOf(this)

        if (this is FirProperty) {
            fun takeIfNecessary(it: FirPropertyAccessor) {
                if (it.visibility.compatibleAndLesser(this.visibility)) {
                    parts.add(it)
                }
            }

            this.getter?.let(::takeIfNecessary)
            this.setter?.let(::takeIfNecessary)
        }

        return parts
    }

    private fun Visibility.compatibleAndLesser(other: Visibility): Boolean {
        val difference = this.compareTo(other) ?: return false
        return difference <= 0
    }

    private fun CheckerContext.containerIsInterface(outerLevel: Int): Boolean {
        return this.getContainerAt(outerLevel)?.classKind?.isInterface == true
    }

    private fun CheckerContext.containerIsNonCompanionObject(outerLevel: Int): Boolean {
        val containingClassSymbol = this.getContainerAt(outerLevel) ?: return false

        @OptIn(SymbolInternals::class)
        val containingClass = containingClassSymbol.fir.safeAs<FirRegularClass>() ?: return false

        return containingClass.classKind == ClassKind.OBJECT && !containingClass.isCompanion
    }

    private fun CheckerContext.getContainerAt(outerLevel: Int): FirClassLikeSymbol<*>? {
        val last = this.containingDeclarations.asReversed().getOrNull(outerLevel)
        return if (last is FirClassLikeDeclaration) {
            last.symbol
        } else {
            null
        }
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
