/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.fullyExpandedClassId
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.SpecialNames

object FirJvmStaticChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirConstructor) {
            // WRONG_DECLARATION_TARGET
            return
        }

        if (declaration is FirPropertyAccessor) {
            return
        }

        val declarationAnnotation = declaration.findAnnotation(JvmStandardClassIds.Annotations.JvmStatic, context.session)

        if (declarationAnnotation != null) {
            checkAnnotated(declaration, context, reporter, declaration.source)
        }

        fun checkIfAnnotated(it: FirDeclaration) {
            if (!it.hasAnnotation(JvmStandardClassIds.Annotations.JvmStatic, context.session)) {
                return
            }
            val targetSource = it.source ?: declaration.source
            checkAnnotated(it, context, reporter, targetSource, declaration as? FirProperty)
        }

        if (declaration is FirProperty) {
            declaration.getter?.let { checkIfAnnotated(it) }
            declaration.setter?.let { checkIfAnnotated(it) }
        }
    }

    private fun checkAnnotated(
        declaration: FirDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        targetSource: KtSourceElement?,
        outerProperty: FirProperty? = null,
    ) {
        if (declaration !is FirMemberDeclaration) {
            return
        }

        val container = context.getContainerAt(0) ?: return
        val supportsJvmStaticInInterface = context.supports(LanguageFeature.JvmStaticInInterface)
        val containerIsAnonymous = container.classId.shortClassName == SpecialNames.ANONYMOUS

        if (
            container.classKind != ClassKind.OBJECT ||
            !container.isCompanion() && containerIsAnonymous
        ) {
            reportStaticNotInProperObject(context, reporter, supportsJvmStaticInInterface, targetSource)
        } else if (
            container.isCompanion() &&
            context.containerIsInterface(1)
        ) {
            if (supportsJvmStaticInInterface) {
                checkForInterface(declaration, context, reporter, targetSource)
            } else {
                reportStaticNotInProperObject(context, reporter, supportsJvmStaticInInterface, targetSource)
            }
        }

        checkOverrideCannotBeStatic(declaration, context, reporter, targetSource, outerProperty)
        checkStaticOnConstOrJvmField(declaration, context, reporter, targetSource)
    }

    private fun reportStaticNotInProperObject(
        context: CheckerContext,
        reporter: DiagnosticReporter,
        supportsJvmStaticInInterface: Boolean,
        targetSource: KtSourceElement?,
    ) {
        val properDiagnostic = if (supportsJvmStaticInInterface) {
            FirJvmErrors.JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION
        } else {
            FirJvmErrors.JVM_STATIC_NOT_IN_OBJECT_OR_CLASS_COMPANION
        }

        reporter.reportOn(targetSource, properDiagnostic, context)
    }

    private fun checkForInterface(
        declaration: FirDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        targetSource: KtSourceElement?,
    ) {
        if (declaration !is FirCallableDeclaration) {
            return
        }

        val visibility = if (declaration is FirProperty) {
            declaration.getMinimumVisibility()
        } else {
            declaration.visibility
        }

        val isExternal = if (declaration is FirProperty) {
            declaration.hasExternalParts()
        } else {
            declaration.isExternal
        }

        if (visibility != Visibilities.Public) {
            reporter.reportOn(targetSource, FirJvmErrors.JVM_STATIC_ON_NON_PUBLIC_MEMBER, context)
        } else if (isExternal) {
            reporter.reportOn(targetSource, FirJvmErrors.JVM_STATIC_ON_EXTERNAL_IN_INTERFACE, context)
        }
    }

    private fun FirProperty.hasExternalParts(): Boolean {
        var hasExternal = isExternal

        getter?.let {
            hasExternal = hasExternal || it.isExternal
        }

        setter?.let {
            hasExternal = hasExternal || it.isExternal
        }

        return hasExternal
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

    private fun checkOverrideCannotBeStatic(
        declaration: FirMemberDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        targetSource: KtSourceElement?,
        outerProperty: FirProperty? = null,
    ) {
        val isOverride = outerProperty?.isOverride ?: declaration.isOverride

        if (!isOverride || !context.containerIsNonCompanionObject(0)) {
            return
        }

        reporter.reportOn(targetSource, FirJvmErrors.OVERRIDE_CANNOT_BE_STATIC, context)
    }

    private fun checkStaticOnConstOrJvmField(
        declaration: FirDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        targetSource: KtSourceElement?,
    ) {
        if (declaration !is FirProperty) return
        if (declaration.isConst || declaration.backingField?.hasAnnotationNamedAs(JvmStandardClassIds.Annotations.JvmField, context.session) == true) {
            reporter.reportOn(targetSource, FirJvmErrors.JVM_STATIC_ON_CONST_OR_JVM_FIELD, context)
        }
    }

    private fun CheckerContext.containerIsInterface(outerLevel: Int): Boolean {
        return this.getContainerAt(outerLevel)?.classKind?.isInterface == true
    }

    private fun CheckerContext.containerIsNonCompanionObject(outerLevel: Int): Boolean {
        val containingClassSymbol = this.getContainerAt(outerLevel) ?: return false
        val containingClass = (containingClassSymbol as? FirRegularClassSymbol) ?: return false
        return containingClass.classKind == ClassKind.OBJECT && !containingClass.isCompanion
    }

    private fun CheckerContext.getContainerAt(outerLevel: Int): FirClassLikeSymbol<*>? {
        val correction = if (this.containingDeclarations.lastOrNull() is FirProperty) {
            1
        } else {
            0
        }
        val last = this.containingDeclarations.asReversed().getOrNull(outerLevel + correction)
        return if (last is FirClassLikeDeclaration) {
            last.symbol
        } else {
            null
        }
    }

    private fun CheckerContext.supports(feature: LanguageFeature) = session.languageVersionSettings.supportsFeature(feature)

    private fun FirClassLikeSymbol<*>.isCompanion() = (this as? FirRegularClassSymbol)?.isCompanion == true

    private fun FirDeclaration.hasAnnotationNamedAs(classId: ClassId, session: FirSession): Boolean {
        return findAnnotation(classId, session) != null
    }

    private fun FirDeclaration.findAnnotation(classId: ClassId, session: FirSession): FirAnnotation? {
        return annotations.firstOrNull {
            it.annotationTypeRef.coneType.fullyExpandedClassId(session) == classId
        }
    }
}
