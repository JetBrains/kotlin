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
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.requireFeatureSupport
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.FIR_NON_SUPPRESSIBLE_ERROR_NAMES
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.declarations.unwrapVarargValue
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
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

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirAnnotationCall) {
        val argumentMapping = expression.argumentMapping.mapping
        val annotationClassId = expression.toAnnotationClassId(context.session)
        val fqName = annotationClassId?.asSingleFqName()
        for (arg in argumentMapping.values) {
            val argExpression = (arg as? FirErrorExpression)?.expression ?: arg
            checkAnnotationArgumentWithSubElements(argExpression, context.session)
                ?.let { reporter.reportOn(argExpression.source, it) }
        }

        checkAnnotationsWithVersion(fqName, expression)
        checkDeprecatedSinceKotlin(expression.source, fqName, argumentMapping)
        checkAnnotationsInsideAnnotationCall(expression)
        checkErrorSuppression(annotationClassId, argumentMapping)
        checkContextFunctionTypeParams(expression.source, annotationClassId)
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkAnnotationArgumentWithSubElements(
        expression: FirExpression,
        session: FirSession,
    ): KtDiagnosticFactory0? {

        fun checkArgumentList(args: FirArgumentList): KtDiagnosticFactory0? {
            var usedNonConst = false

            for (arg in args.arguments) {
                val sourceForReport = arg.source

                when (val err = checkAnnotationArgumentWithSubElements(arg, session)) {
                    null -> {
                        //DO NOTHING
                    }
                    else -> {
                        if (err != FirErrors.ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL) usedNonConst = true
                        reporter.reportOn(sourceForReport, err)
                    }
                }
            }

            return if (usedNonConst) FirErrors.NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION
            else null
        }

        when (expression) {
            is FirCollectionLiteral -> return checkArgumentList(expression.argumentList)
            is FirVarargArgumentsExpression -> {
                for (arg in expression.arguments) {
                    val unwrappedArg = arg.unwrapArgument()
                    checkAnnotationArgumentWithSubElements(unwrappedArg, session)
                        ?.let { reporter.reportOn(unwrappedArg.source, it) }
                }
            }
            else -> {
                return when (computeConstantExpressionKind(expression, session, calledOnCheckerStage = true)) {
                    ConstantArgumentKind.NOT_CONST -> FirErrors.ANNOTATION_ARGUMENT_MUST_BE_CONST
                    ConstantArgumentKind.ENUM_NOT_CONST -> FirErrors.ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST
                    ConstantArgumentKind.NOT_KCLASS_LITERAL -> FirErrors.ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL
                    ConstantArgumentKind.KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR -> FirErrors.ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR
                    ConstantArgumentKind.NOT_CONST_VAL_IN_CONST_EXPRESSION -> FirErrors.NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION
                    ConstantArgumentKind.VALID_CONST, ConstantArgumentKind.RESOLUTION_ERROR ->
                        //try to go deeper if we are not sure about this function call
                        //to report non-constant val in not fully resolved calls
                        if (expression is FirFunctionCall) checkArgumentList(expression.argumentList)
                        else null
                }
            }
        }
        return null
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun parseVersionExpressionOrReport(
        expression: FirExpression,
    ): ApiVersion? {
        val constantExpression = (expression as? FirLiteralExpression) ?: return null
        val stringValue = constantExpression.value as? String ?: return null
        if (!stringValue.matches(RequireKotlinConstants.VERSION_REGEX)) {
            reporter.reportOn(expression.source, FirErrors.ILLEGAL_KOTLIN_VERSION_STRING_VALUE)
            return null
        }
        val version = ApiVersion.parse(stringValue)
        if (version == null) {
            reporter.reportOn(expression.source, FirErrors.ILLEGAL_KOTLIN_VERSION_STRING_VALUE)
        }
        return version
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAnnotationsWithVersion(
        fqName: FqName?,
        annotation: FirAnnotation,
    ) {
        if (!annotationFqNamesWithVersion.contains(fqName)) return
        val versionExpression = annotation.findArgumentByName(versionArgumentName) ?: return
        val version = parseVersionExpressionOrReport(versionExpression) ?: return
        if (fqName == sinceKotlinFqName) {
            val specified = context.session.languageVersionSettings.apiVersion
            if (version > specified) {
                reporter.reportOn(versionExpression.source, FirErrors.NEWER_VERSION_IN_SINCE_KOTLIN, specified.versionString)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDeprecatedSinceKotlin(
        source: KtSourceElement?,
        fqName: FqName?,
        argumentMapping: Map<Name, FirExpression>,
    ) {
        if (fqName != deprecatedSinceKotlinFqName)
            return

        if (argumentMapping.size == 0) {
            reporter.reportOn(source, FirErrors.DEPRECATED_SINCE_KOTLIN_WITHOUT_ARGUMENTS)
        }

        var warningSince: ApiVersion? = null
        var errorSince: ApiVersion? = null
        var hiddenSince: ApiVersion? = null
        for ((name, argument) in argumentMapping) {
            val identifier = name.identifier
            if (identifier == "warningSince" || identifier == "errorSince" || identifier == "hiddenSince") {
                val version = parseVersionExpressionOrReport(argument)
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
            reporter.reportOn(source, FirErrors.DEPRECATED_SINCE_KOTLIN_WITH_UNORDERED_VERSIONS)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAnnotationsInsideAnnotationCall(
        expression: FirCall,
    ) {
        val args = expression.argumentList.arguments
        for (arg in args) {
            val unwrapped = arg.unwrapArgument()
            val unwrappedErrorExpression = unwrapped.unwrapErrorExpression()
            val errorFactory = if (unwrapped is FirErrorExpression && unwrapped.expression == null) {
                // The error is reported if only a syntax error is reported as well (empty element) that leads to some duplication.
                // However, the `ANNOTATION_USED_AS_ANNOTATION_ARGUMENT` allows applying the `RemoveAtFromAnnotationArgument` quick-fix.
                // That's why it's useful to have it.
                FirErrors.ANNOTATION_USED_AS_ANNOTATION_ARGUMENT
            } else {
                FirErrors.ANNOTATION_ON_ANNOTATION_ARGUMENT
            }
            for (ann in unwrappedErrorExpression.annotations) {
                reporter.reportOn(ann.source, errorFactory)
            }
            if (unwrappedErrorExpression is FirCollectionLiteral) {
                checkAnnotationsInsideAnnotationCall(unwrappedErrorExpression)
            }
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkErrorSuppression(
        annotationClassId: ClassId?,
        argumentMapping: Map<Name, FirExpression>,
    ) {
        if (context.languageVersionSettings.getFlag(AnalysisFlags.dontWarnOnErrorSuppression)) return
        if (annotationClassId != StandardClassIds.Annotations.Suppress) return
        val nameExpressions = argumentMapping[StandardClassIds.Annotations.ParameterNames.suppressNames]?.unwrapVarargValue() ?: return
        for (nameExpression in nameExpressions) {
            val name = (nameExpression as? FirLiteralExpression)?.value as? String ?: continue
            val parameter = when (name) {
                in FIR_NON_SUPPRESSIBLE_ERROR_NAMES -> name
                AbstractDiagnosticCollector.SUPPRESS_ALL_ERRORS -> "all errors"
                else -> continue
            }
            reporter.reportOn(nameExpression.source, FirErrors.ERROR_SUPPRESSION, parameter)
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkContextFunctionTypeParams(
        source: KtSourceElement?,
        annotationClassId: ClassId?,
    ) {
        if (annotationClassId != StandardClassIds.Annotations.ContextFunctionTypeParams) return
        source.requireFeatureSupport(LanguageFeature.ContextReceivers)
    }
}
