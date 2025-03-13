/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.JvmFieldApplicabilityProblem.*
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageFeature.ForbidFieldAnnotationsOnAnnotationParameters
import org.jetbrains.kotlin.config.LanguageFeature.ForbidJvmAnnotationsOnAnnotationParameters
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitJvmFieldOnOverrideFromInterfaceInPrimaryConstructor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.needsMultiFieldValueClassFlattening
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.processAllDeclaredCallables
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_MULTIFILE_CLASS_ID
import org.jetbrains.kotlin.name.StandardClassIds

object FirJvmFieldApplicabilityChecker : FirPropertyChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val session = context.session
        val annotation = declaration.backingField?.getAnnotationByClassId(JVM_FIELD_ANNOTATION_CLASS_ID, session)
            ?: return
        val containingClassSymbol = declaration.containingClassLookupTag()?.toRegularClassSymbol(session)

        val problem = when {
            declaration.delegate != null -> DELEGATE
            !declaration.hasBackingField -> return
            declaration.isOverridable(containingClassSymbol) -> NOT_FINAL
            Visibilities.isPrivate(declaration.visibility) -> PRIVATE
            declaration.hasCustomAccessor() -> CUSTOM_ACCESSOR
            declaration.isOverride -> OVERRIDES
            declaration.isLateInit -> LATEINIT
            declaration.isConst -> CONST
            containingClassSymbol != null && containingClassSymbol.isInsideCompanionObjectOfInterface(session) -> {
                if (!session.languageVersionSettings.supportsFeature(LanguageFeature.JvmFieldInInterface)) {
                    INSIDE_COMPANION_OF_INTERFACE
                } else {
                    if (!isInterfaceCompanionWithPublicJvmFieldProperties(containingClassSymbol, session)) {
                        NOT_PUBLIC_VAL_WITH_JVMFIELD
                    } else {
                        return
                    }
                }
            }
            containingClassSymbol == null && isInsideJvmMultifileClassFile(context) ->
                TOP_LEVEL_PROPERTY_OF_MULTIFILE_FACADE
            declaration.returnTypeRef.isInlineClassThatRequiresMangling(session) -> RETURN_TYPE_IS_VALUE_CLASS
            declaration.returnTypeRef.needsMultiFieldValueClassFlattening(session) -> RETURN_TYPE_IS_VALUE_CLASS
            containingClassSymbol?.classKind == ClassKind.ANNOTATION_CLASS -> ANNOTATION
            else -> return
        }

        val languageVersionSettings = context.session.languageVersionSettings
        val factory = when {
            declaration.fromPrimaryConstructor == true &&
                    !languageVersionSettings.supportsFeature(ProhibitJvmFieldOnOverrideFromInterfaceInPrimaryConstructor)
                -> {
                FirJvmErrors.INAPPLICABLE_JVM_FIELD_WARNING
            }
            problem == ANNOTATION -> {
                when {
                    !languageVersionSettings.supportsFeature(ForbidJvmAnnotationsOnAnnotationParameters) ->
                        FirJvmErrors.INAPPLICABLE_JVM_FIELD_WARNING
                    languageVersionSettings.supportsFeature(ForbidFieldAnnotationsOnAnnotationParameters) ->
                        return
                    else ->
                        FirJvmErrors.INAPPLICABLE_JVM_FIELD
                }
            }
            else -> {
                FirJvmErrors.INAPPLICABLE_JVM_FIELD
            }
        }

        reporter.reportOn(annotation.source, factory, problem.errorMessage, context)
    }

    private fun FirTypeRef.isInlineClassThatRequiresMangling(session: FirSession): Boolean {
        val symbol = this.coneType.toRegularClassSymbol(session) ?: return false
        return symbol.isInlineOrValue && !symbol.isDontMangleClass()
    }

    private fun FirRegularClassSymbol.isDontMangleClass(): Boolean {
        return this.classId == StandardClassIds.Result
    }

    private fun FirProperty.isOverridable(containingClass: FirRegularClassSymbol?): Boolean {
        return visibility != Visibilities.Private && modality != Modality.FINAL &&
                containingClass?.isFinal != true
    }

    private fun FirProperty.hasCustomAccessor(): Boolean {
        return (getter != null && getter?.source?.kind !is KtFakeSourceElementKind) ||
                (setter != null && setter?.source?.kind !is KtFakeSourceElementKind)
    }

    private fun FirRegularClassSymbol.isInsideCompanionObjectOfInterface(session: FirSession): Boolean {
        if (!isCompanion) {
            return false
        }

        val outerClassKind = getContainingDeclaration(session)?.classKind
        return outerClassKind == ClassKind.INTERFACE || outerClassKind == ClassKind.ANNOTATION_CLASS
    }

    private fun isInterfaceCompanionWithPublicJvmFieldProperties(containingClass: FirRegularClassSymbol, session: FirSession): Boolean {
        var result = true
        containingClass.processAllDeclaredCallables(session) { symbol ->
            if (!result || symbol !is FirPropertySymbol) return@processAllDeclaredCallables

            if (symbol.visibility != Visibilities.Public ||
                symbol.isVar ||
                symbol.modality != Modality.FINAL ||
                !symbol.hasJvmFieldAnnotation(session)
            ) {
                result = false
            }
        }

        return result
    }

    private fun FirPropertySymbol.hasJvmFieldAnnotation(session: FirSession): Boolean {
        return backingFieldSymbol?.getAnnotationByClassId(JVM_FIELD_ANNOTATION_CLASS_ID, session) != null
    }

    private fun isInsideJvmMultifileClassFile(context: CheckerContext): Boolean {
        return context.containingFile?.annotations?.any {
            it.annotationTypeRef.coneType.classId == JVM_MULTIFILE_CLASS_ID
        } == true
    }
}
