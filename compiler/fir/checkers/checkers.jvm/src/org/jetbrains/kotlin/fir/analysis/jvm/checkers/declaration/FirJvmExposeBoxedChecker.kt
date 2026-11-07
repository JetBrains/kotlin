/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration


import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalDeclaredInBlock
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.java.findJvmNameValue
import org.jetbrains.kotlin.fir.propertyIfAccessor
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.isValidJavaIdentifier

object FirJvmExposeBoxedChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override val platformSpecificCheckerEnabledInMetadataCompilation: Boolean
        get() = true

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

            val value = (name as? FirLiteralExpression)?.value as? String
            if (value != null) {
                if (!Name.isValidIdentifier(value)) {
                    reporter.reportOn(name.source, FirJvmErrors.ILLEGAL_JVM_NAME)
                } else if (!isValidJavaIdentifier(value)) {
                    reporter.reportOn(name.source, FirJvmErrors.JVM_EXPOSE_BOXED_NAME_IS_NOT_JAVA_IDENTIFIER, value)
                }
            }

            if (declaration is FirFunction && declaration.nameOrSpecialName.asString() == value) {
                reporter.reportOn(name.source, FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME)
            }
        }

        if (declaration is FirClass) {
            if (declaration.isInterface) {
                reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_OPEN_ABSTRACT)
            }

            if (declaration.effectiveVisibility.privateApi) {
                reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE)
            }
        }

        if (declaration is FirCallableDeclaration) {
            val cannotExposeErrors = cannotExposeErrors(declaration)
            for (error in cannotExposeErrors) {
                reporter.reportOn(jvmExposeBoxedAnnotation.source, error)
            }

            if (!declaration.isWithInlineClass(context.session)) {
                if (name == null || declaration.cannotRename()) {
                    // An error above already rejects the annotation, so the warning only repeats it.
                    if (cannotExposeErrors.isEmpty()) {
                        reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.USELESS_JVM_EXPOSE_BOXED)
                    }
                } else if (FirJvmNameChecker.isApplicableTo(declaration)) {
                    // Do not suggest `@JvmName` where it is not applicable, for example, on an override.
                    reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.JVM_EXPOSE_BOXED_CAN_BE_REPLACED_WITH_JVM_NAME)
                }
            } else if (name == null && !declaration.isMangledOrWithResult(context.session)) {
                if (declaration is FirFunction) {
                    reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.JVM_EXPOSE_BOXED_REQUIRES_NAME)
                }
            }

            if (!declaration.canBeOverloadedByExposed(context.session)) {
                checkJvmNameHasDifferentName(name, declaration)
            }
        }
    }

    /**
     * Returns the `JVM_EXPOSE_BOXED_CANNOT_EXPOSE_*` errors for `@JvmExposeBoxed` on [declaration].
     */
    context(context: CheckerContext)
    private fun cannotExposeErrors(declaration: FirCallableDeclaration): List<KtDiagnosticFactory0> = buildList {
        if (declaration.hasAnnotation(JvmStandardClassIds.JVM_SYNTHETIC_ANNOTATION_CLASS_ID, context.session)) {
            add(FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SYNTHETIC)
        }

        if (!declaration.isFinal && declaration.containingClassLookupTag()?.toRegularClassSymbol()?.isFinal == false) {
            add(FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_OPEN_ABSTRACT)
        }

        if (declaration.propertyIfAccessor.typeParameters.any { it.symbol.isReified }) {
            add(FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_REIFIED)
        }

        if (declaration is FirFunction && declaration.isSuspend) {
            add(FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SUSPEND)
        }

        if (declaration.isLocalDeclaredInBlock) {
            add(FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_LOCALS)
        }

        if (declaration.effectiveVisibility.privateApi) {
            add(FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE)
        }

        if (declaration is FirConstructor && declaration.isFromSealedClass) {
            add(FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SEALED_CONSTRUCTOR)
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkJvmNameHasDifferentName(
        name: FirExpression?,
        declaration: FirDeclaration,
    ) {
        if (name == null) return
        val value = (name as? FirLiteralExpression)?.value as? String ?: return

        if (value == declaration.findJvmNameValue()) {
            reporter.reportOn(
                name.source,
                FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME_AS_JVM_NAME
            )
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
        if (propertyIfAccessor.receiverParameter?.typeRef?.isInline(session) == true) return true
        if (propertyIfAccessor.contextParameters.any { it.returnTypeRef.isInline(session) }) return true
        if (this is FirFunction && valueParameters.any { it.returnTypeRef.isInline(session) }) return true
        // Check dispatch receiver as well - we use `-impl` suffix for them
        if (this !is FirConstructor) {
            val containingClass = containingClassLookupTag()?.toRegularClassSymbol(session)
            return containingClass?.isInlineClass == true
        }
        return false
    }

    private fun FirTypeRef.isInline(session: FirSession): Boolean {
        val classSymbol = toRegularClassSymbol(session) ?: return false
        return classSymbol.isInlineClass
    }
}
