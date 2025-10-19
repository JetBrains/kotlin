/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration


import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalMember
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.java.findJvmNameValue
import org.jetbrains.kotlin.fir.propertyIfAccessor
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name

object FirJvmExposeBoxedChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        val jvmExposeBoxedAnnotation =
            declaration.getAnnotationByClassId(JvmStandardClassIds.JVM_EXPOSE_BOXED_ANNOTATION_CLASS_ID, context.session)

        if (jvmExposeBoxedAnnotation != null) {
            checkJvmExposeBoxedAnnotation(jvmExposeBoxedAnnotation, declaration)
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkJvmExposeBoxedAnnotation(
        jvmExposeBoxedAnnotation: FirAnnotation,
        declaration: FirDeclaration,
    ) {
        val name = jvmExposeBoxedAnnotation.findArgumentByName(JvmStandardClassIds.Annotations.ParameterNames.jvmExposeBoxedName)

        if (name != null) {
            if (declaration.cannotRename()) {
                reporter.reportOn(name.source, FirJvmErrors.INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME)
            }

            val value = name.evaluateAs<FirLiteralExpression>(context.session)?.value as? String
            if (value != null && !Name.isValidIdentifier(value)) {
                reporter.reportOn(name.source, FirJvmErrors.ILLEGAL_JVM_NAME)
            }
        }

        if (declaration is FirClass && declaration.isInterface) {
            reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_OPEN_ABSTRACT)
        }

        if (declaration is FirCallableDeclaration) {
            if (!declaration.isWithInlineClass(context.session)) {
                reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.USELESS_JVM_EXPOSE_BOXED)
            } else if (name == null && !declaration.isMangledOrWithResult(context.session)) {
                if (declaration is FirFunction) {
                    reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.JVM_EXPOSE_BOXED_REQUIRES_NAME)
                }
            }

            if (declaration.hasAnnotation(JvmStandardClassIds.JVM_SYNTHETIC_ANNOTATION_CLASS_ID, context.session)) {
                reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SYNTHETIC)
            }

            if (!declaration.isFinal && declaration.containingClassLookupTag()?.toRegularClassSymbol()?.isFinal == false) {
                reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_OPEN_ABSTRACT)
            }

            if (!declaration.canBeOverloadedByExposed(context.session)) {
                checkJvmNameHasDifferentName(name, declaration)
            }

            if (declaration.propertyIfAccessor.typeParameters.any { it.symbol.isReified }) {
                reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_REIFIED)
            }

            if (declaration is FirFunction && declaration.isSuspend) {
                reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SUSPEND)
            }

            if (declaration.isLocalMember) {
                reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_LOCALS)
            }
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkJvmNameHasDifferentName(
        name: FirExpression?,
        declaration: FirDeclaration,
    ) {
        if (name == null) return
        val value = name.evaluateAs<FirLiteralExpression>(context.session)?.value as? String ?: return

        if (value == declaration.findJvmNameValue()) {
            reporter.reportOn(
                name.source,
                FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME_AS_JVM_NAME
            )
        }

        if (declaration is FirFunction && declaration.nameOrSpecialName.asString() == value) {
            reporter.reportOn(name.source, FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME)
        }
    }

    private fun FirDeclaration.cannotRename(): Boolean = this is FirClass || this is FirConstructor

    private fun FirCallableDeclaration.isWithInlineClass(session: FirSession): Boolean {
        if (canBeOverloadedByExposed(session)) return true
        if (returnTypeRef.isInline(session)) return true
        return false
    }

    /* Just like [isMangled], but takes into account little quirks like globals, returning inline class. */
    private fun FirCallableDeclaration.isMangledOrWithResult(session: FirSession): Boolean {
        if (canBeOverloadedByExposed(session)) return true
        val containingClass = containingClassLookupTag()?.toRegularClassSymbol(session)
        if (containingClass != null) return returnTypeRef.isInline(session)
        return false
    }

    // If the inline class is not return type, it is safe to name both boxed and unboxed versions the same.
    private fun FirCallableDeclaration.canBeOverloadedByExposed(session: FirSession): Boolean {
        // Inline in extension receiver or parameters -> safe to have both boxed and unboxed versions
        if (receiverParameter?.typeRef?.isInline(session) == true) return true
        if (contextParameters.any { it.returnTypeRef.isInline(session) }) return true
        if (this is FirFunction && valueParameters.any { it.returnTypeRef.isInline(session) }) return true
        // Consider dispatch receiver: member of a value class
        val containing = containingClassLookupTag()?.toRegularClassSymbol(session)
        if (containing?.isInlineOrValue == true && this !is FirConstructor) return true
        return false
    }

    private fun FirTypeRef.isInline(session: FirSession): Boolean =
        toRegularClassSymbol(session)?.isInlineOrValue ?: false
}