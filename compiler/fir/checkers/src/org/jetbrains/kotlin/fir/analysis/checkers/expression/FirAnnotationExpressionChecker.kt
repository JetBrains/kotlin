/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.ConstantArgumentKind
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkConstantArguments
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.FIR_NON_SUPPRESSIBLE_ERROR_NAMES
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.declarations.unwrapVarargValue
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.RequireKotlinConstants

object FirAnnotationExpressionChecker : FirAnnotationCallChecker(MppCheckerKind.Common) {
    private val versionArgumentName = Name.identifier("version")
    private val deprecatedSinceKotlinFqName = FqName("kotlin.DeprecatedSinceKotlin")
    private val sinceKotlinFqName = FqName("kotlin.SinceKotlin")

    private val annotationFqNamesWithVersion = setOf(
        RequireKotlinConstants.FQ_NAME,
        sinceKotlinFqName,
    )

    override fun check(expression: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val argumentMapping = expression.argumentMapping.mapping
        val annotationClassId = expression.toAnnotationClassId(context.session)
        val fqName = annotationClassId?.asSingleFqName()
        for (arg in argumentMapping.values) {
            val argExpression = (arg as? FirNamedArgumentExpression)?.expression ?: (arg as? FirErrorExpression)?.expression ?: arg
            checkAnnotationArgumentWithSubElements(argExpression, context.session, reporter, context)
                ?.let { reporter.reportOn(argExpression.source, it, context) }
        }

        checkAnnotationsWithVersion(fqName, expression, context, reporter)
        checkDeprecatedSinceKotlin(expression.source, fqName, argumentMapping, context, reporter)
        checkAnnotationUsedAsAnnotationArgument(expression, context, reporter)
        checkNotAClass(expression, context, reporter)
        checkErrorSuppression(annotationClassId, argumentMapping, reporter, context)
        checkContextFunctionTypeParams(expression.source, annotationClassId, reporter, context)
    }

    private fun checkAnnotationArgumentWithSubElements(
        expression: FirExpression,
        session: FirSession,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ): KtDiagnosticFactory0? {

        fun checkArgumentList(args: FirArgumentList): KtDiagnosticFactory0? {
            var usedNonConst = false

            for (arg in args.arguments) {
                val sourceForReport = arg.source

                when (val err = checkAnnotationArgumentWithSubElements(arg, session, reporter, context)) {
                    null -> {
                        //DO NOTHING
                    }
                    else -> {
                        if (err != FirErrors.ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL) usedNonConst = true
                        reporter.reportOn(sourceForReport, err, context)
                    }
                }
            }

            return if (usedNonConst) FirErrors.NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION
            else null
        }

        when (expression) {
            is FirArrayLiteral -> return checkArgumentList(expression.argumentList)
            is FirVarargArgumentsExpression -> {
                for (arg in expression.arguments) {
                    val unwrappedArg = arg.unwrapArgument()
                    checkAnnotationArgumentWithSubElements(unwrappedArg, session, reporter, context)
                        ?.let { reporter.reportOn(unwrappedArg.source, it, context) }
                }
            }
            else -> {
                return when (checkConstantArguments(expression, session)) {
                    ConstantArgumentKind.NOT_CONST -> FirErrors.ANNOTATION_ARGUMENT_MUST_BE_CONST
                    ConstantArgumentKind.ENUM_NOT_CONST -> FirErrors.ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST
                    ConstantArgumentKind.NOT_KCLASS_LITERAL -> FirErrors.ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL
                    ConstantArgumentKind.KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR -> FirErrors.ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR
                    ConstantArgumentKind.NOT_CONST_VAL_IN_CONST_EXPRESSION -> FirErrors.NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION
                    ConstantArgumentKind.VALID_CONST ->
                        //try to go deeper if we are not sure about this function call
                        //to report non-constant val in not fully resolved calls
                        if (expression is FirFunctionCall) checkArgumentList(expression.argumentList)
                        else null
                }
            }
        }
        return null
    }

    private fun parseVersionExpressionOrReport(
        expression: FirExpression,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ): ApiVersion? {
        val constantExpression = (expression as? FirLiteralExpression<*>)
            ?: ((expression as? FirNamedArgumentExpression)?.expression as? FirLiteralExpression<*>) ?: return null
        val stringValue = constantExpression.value as? String ?: return null
        if (!stringValue.matches(RequireKotlinConstants.VERSION_REGEX)) {
            reporter.reportOn(expression.source, FirErrors.ILLEGAL_KOTLIN_VERSION_STRING_VALUE, context)
            return null
        }
        val version = ApiVersion.parse(stringValue)
        if (version == null) {
            reporter.reportOn(expression.source, FirErrors.ILLEGAL_KOTLIN_VERSION_STRING_VALUE, context)
        }
        return version
    }

    private fun checkAnnotationsWithVersion(
        fqName: FqName?,
        annotation: FirAnnotation,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (!annotationFqNamesWithVersion.contains(fqName)) return
        val versionExpression = annotation.findArgumentByName(versionArgumentName) ?: return
        val version = parseVersionExpressionOrReport(versionExpression, context, reporter) ?: return
        if (fqName == sinceKotlinFqName) {
            val specified = context.session.languageVersionSettings.apiVersion
            if (version > specified) {
                reporter.reportOn(versionExpression.source, FirErrors.NEWER_VERSION_IN_SINCE_KOTLIN, specified.versionString, context)
            }
        }
    }

    private fun checkDeprecatedSinceKotlin(
        source: KtSourceElement?,
        fqName: FqName?,
        argumentMapping: Map<Name, FirExpression>,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (fqName != deprecatedSinceKotlinFqName)
            return

        if (argumentMapping.size == 0) {
            reporter.reportOn(source, FirErrors.DEPRECATED_SINCE_KOTLIN_WITHOUT_ARGUMENTS, context)
        }

        var warningSince: ApiVersion? = null
        var errorSince: ApiVersion? = null
        var hiddenSince: ApiVersion? = null
        for ((name, argument) in argumentMapping) {
            val identifier = name.identifier
            if (identifier == "warningSince" || identifier == "errorSince" || identifier == "hiddenSince") {
                val version = parseVersionExpressionOrReport(argument, context, reporter)
                if (version != null) {
                    when (identifier) {
                        "warningSince" -> warningSince = version
                        "errorSince" -> errorSince = version
                        "hiddenSince" -> hiddenSince = version
                    }
                }
            }
        }

        var isReportDeprecatedSinceKotlinWithUnorderedVersions = false
        if (warningSince != null) {
            if (errorSince != null) {
                isReportDeprecatedSinceKotlinWithUnorderedVersions = warningSince > errorSince
            }

            if (hiddenSince != null && !isReportDeprecatedSinceKotlinWithUnorderedVersions) {
                isReportDeprecatedSinceKotlinWithUnorderedVersions = warningSince > hiddenSince
            }
        }

        if (errorSince != null && hiddenSince != null && !isReportDeprecatedSinceKotlinWithUnorderedVersions) {
            isReportDeprecatedSinceKotlinWithUnorderedVersions = errorSince > hiddenSince
        }

        if (isReportDeprecatedSinceKotlinWithUnorderedVersions) {
            reporter.reportOn(source, FirErrors.DEPRECATED_SINCE_KOTLIN_WITH_UNORDERED_VERSIONS, context)
        }
    }

    private fun checkAnnotationUsedAsAnnotationArgument(
        expression: FirAnnotationCall,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val args = expression.argumentList.arguments
        for (arg in args) {
            for (ann in arg.unwrapArgument().annotations) {
                reporter.reportOn(ann.source, FirErrors.ANNOTATION_USED_AS_ANNOTATION_ARGUMENT, context)
            }
        }
    }

    private fun checkNotAClass(
        expression: FirAnnotationCall,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val annotationTypeRef = expression.annotationTypeRef
        if (expression.calleeReference is FirErrorNamedReference &&
            annotationTypeRef !is FirErrorTypeRef &&
            annotationTypeRef.coneType !is ConeClassLikeType
        ) {
            reporter.reportOn(annotationTypeRef.source, FirErrors.NOT_A_CLASS, context)
        }
    }

    private fun checkErrorSuppression(
        annotationClassId: ClassId?,
        argumentMapping: Map<Name, FirExpression>,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {
        if (context.languageVersionSettings.getFlag(AnalysisFlags.dontWarnOnErrorSuppression)) return
        if (annotationClassId != StandardClassIds.Annotations.Suppress) return
        val nameExpressions = argumentMapping[StandardClassIds.Annotations.ParameterNames.suppressNames]?.unwrapVarargValue() ?: return
        for (nameExpression in nameExpressions) {
            val name = (nameExpression as? FirLiteralExpression<*>)?.value as? String ?: continue
            val parameter = when (name) {
                in FIR_NON_SUPPRESSIBLE_ERROR_NAMES -> name
                AbstractDiagnosticCollector.SUPPRESS_ALL_ERRORS -> "all errors"
                else -> continue
            }
            reporter.reportOn(nameExpression.source, FirErrors.ERROR_SUPPRESSION, parameter, context)
        }
    }

    private fun checkContextFunctionTypeParams(
        source: KtSourceElement?,
        annotationClassId: ClassId?,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)) return
        if (annotationClassId != StandardClassIds.Annotations.ContextFunctionTypeParams) return
        reporter.reportOn(
            source,
            FirErrors.UNSUPPORTED_FEATURE,
            LanguageFeature.ContextReceivers to context.languageVersionSettings,
            context
        )
    }
}
