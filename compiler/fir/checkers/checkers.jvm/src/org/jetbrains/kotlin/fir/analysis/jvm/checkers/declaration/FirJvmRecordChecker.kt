/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmNames.JVM_RECORD_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.name.StandardClassIds

object FirJvmRecordChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        declaration.superTypeRefs.firstOrNull()?.let { typeRef ->
            // compiler automatically adds java.lang.Record supertype, so we should check only for explicit type declarations
            if (typeRef.source != null && typeRef.coneTypeSafe<ConeClassLikeType>()?.classId == StandardClassIds.Java.Record) {
                reporter.reportOn(typeRef.source, FirJvmErrors.ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE, context)
                return
            }
        }

        val annotationSource = declaration.getAnnotationByClassId(JVM_RECORD_ANNOTATION_CLASS_ID)?.source ?: return

        val languageVersionSettings = context.session.languageVersionSettings
        if (!languageVersionSettings.supportsFeature(LanguageFeature.JvmRecordSupport)) {
            reporter.reportOn(
                annotationSource,
                FirErrors.UNSUPPORTED_FEATURE,
                LanguageFeature.JvmRecordSupport to languageVersionSettings,
                context
            )
            return
        }

        if (declaration.isLocal) {
            reporter.reportOn(annotationSource, FirJvmErrors.LOCAL_JVM_RECORD, context)
            return
        }

        if (declaration.isInner) {
            reporter.reportOn(declaration.source, FirJvmErrors.INNER_JVM_RECORD, context)
            return
        }

        if (!declaration.isFinal) {
            reporter.reportOn(declaration.source, FirJvmErrors.NON_FINAL_JVM_RECORD, context)
            return
        }

        if (declaration.isEnumClass) {
            reporter.reportOn(declaration.source, FirJvmErrors.ENUM_JVM_RECORD, context)
            return
        }

        if (!declaration.isData) {
            reporter.reportOn(annotationSource, FirJvmErrors.NON_DATA_CLASS_JVM_RECORD, context)
            return
        }

        declaration.primaryConstructorIfAny(context.session)?.valueParameterSymbols?.let { params ->
            if (params.isEmpty()) {
                reporter.reportOn(annotationSource, FirJvmErrors.JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS, context)
                return
            }
            params.dropLast(1).forEach { param ->
                if (param.isVararg) {
                    reporter.reportOn(param.source, FirJvmErrors.JVM_RECORD_NOT_LAST_VARARG_PARAMETER, context)
                }
            }
        }

        declaration.declarations.forEach { decl ->
            if (decl is FirProperty) {
                val fromConstructor = decl.source?.kind == KtFakeSourceElementKind.PropertyFromParameter
                if (decl.isVar && fromConstructor) {
                    reporter.reportOn(decl.source, FirJvmErrors.JVM_RECORD_NOT_VAL_PARAMETER, context)
                } else if (!fromConstructor && (decl.hasBackingField || decl.delegateFieldSymbol != null)) {
                    reporter.reportOn(decl.source, FirJvmErrors.FIELD_IN_JVM_RECORD, context)
                }
            } else if (decl is FirField && decl.isSynthetic) {
                reporter.reportOn(decl.source, FirJvmErrors.DELEGATION_BY_IN_JVM_RECORD, context)
            }
        }

        declaration.superTypeRefs.firstOrNull()?.let { typeRef ->
            if (typeRef.source?.kind != KtRealSourceElementKind) return@let
            if (typeRef.toRegularClassSymbol(context.session)?.classKind == ClassKind.CLASS) {
                reporter.reportOn(declaration.source, FirJvmErrors.JVM_RECORD_EXTENDS_CLASS, typeRef.coneType, context)
            }
        }
    }
}
