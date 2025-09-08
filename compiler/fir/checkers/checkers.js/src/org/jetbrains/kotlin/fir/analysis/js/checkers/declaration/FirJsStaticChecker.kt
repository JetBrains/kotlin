/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
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
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JsStandardClassIds

object FirJsStaticChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (declaration is FirConstructor) {
            // WRONG_DECLARATION_TARGET
            return
        }

        if (declaration is FirPropertyAccessor) {
            return
        }

        val declarationAnnotation = declaration.findAnnotation(JsStandardClassIds.Annotations.JsStatic, context.session)

        if (declarationAnnotation != null) {
            checkAnnotated(declaration, declaration.source)
        }

        fun checkIfAnnotated(it: FirDeclaration) {
            if (!it.hasAnnotation(JsStandardClassIds.Annotations.JsStatic, context.session)) {
                return
            }
            val targetSource = it.source ?: declaration.source
            checkAnnotated(it, targetSource)
        }

        if (declaration is FirProperty) {
            declaration.getter?.let { checkIfAnnotated(it) }
            declaration.setter?.let { checkIfAnnotated(it) }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAnnotated(
        declaration: FirDeclaration,
        targetSource: KtSourceElement?,
    ) {
        if (declaration !is FirMemberDeclaration) {
            return
        }

        val container = declaration.getContainingClassSymbol() ?: return

        if (
            !container.isCompanion() || (
                    container.containerIsInterface() &&
                    !context.languageVersionSettings.supportsFeature(LanguageFeature.JsStaticInInterface)
            )
        ) {
            reporter.reportOn(targetSource, FirJsErrors.JS_STATIC_NOT_IN_CLASS_COMPANION)
        }

        checkStaticOnConst(declaration, targetSource)
        checkVisibility(declaration, targetSource)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkVisibility(
        declaration: FirDeclaration,
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

        if (visibility != Visibilities.Public) {
            reporter.reportOn(targetSource, FirJsErrors.JS_STATIC_ON_NON_PUBLIC_MEMBER)
        }
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

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkStaticOnConst(
        declaration: FirDeclaration,
        targetSource: KtSourceElement?,
    ) {
        if (declaration !is FirProperty) return
        if (declaration.isConst) reporter.reportOn(targetSource, FirJsErrors.JS_STATIC_ON_CONST)
    }

    private fun FirClassLikeSymbol<*>.containerIsInterface(): Boolean {
        return getContainingClassSymbol()?.classKind?.isInterface == true
    }

    private fun FirClassLikeSymbol<*>.isCompanion() = (this as? FirRegularClassSymbol)?.isCompanion == true

    private fun FirDeclaration.findAnnotation(classId: ClassId, session: FirSession): FirAnnotation? {
        return annotations.firstOrNull {
            it.annotationTypeRef.coneType.fullyExpandedClassId(session) == classId
        }
    }
}
