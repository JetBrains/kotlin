/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.ConstantArgumentKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkConstantArguments
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticFactory0
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.RequireKotlinConstants

object FirAnnotationArgumentChecker : FirAnnotationCallChecker() {
    private val deprecatedSinceKotlinFqName = FqName("kotlin.DeprecatedSinceKotlin")
    private val sinceKotlinFqName = FqName("kotlin.SinceKotlin")

    private val annotationFqNamesWithVersion = setOf(
        FqName("kotlin.internal.RequireKotlin"),
        sinceKotlinFqName,
        deprecatedSinceKotlinFqName
    )

    override fun check(expression: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val argumentMapping = expression.argumentMapping ?: return
        val fqName = expression.fqName(context.session)
        for ((arg, _) in argumentMapping) {
            val argExpression = (arg as? FirNamedArgumentExpression)?.expression ?: arg
            checkAnnotationArgumentWithSubElements(argExpression, fqName, context.session, reporter, context)
                ?.let { reporter.reportOn(argExpression.source, it, context) }
        }

        checkDeprecatedSinceKotlin(expression.source, fqName, argumentMapping, context, reporter)

        val args = expression.argumentList.arguments
        for (arg in args) {
            for (ann in arg.unwrapArgument().annotations) {
                reporter.reportOn(ann.source, FirErrors.ANNOTATION_USED_AS_ANNOTATION_ARGUMENT, context)
            }
        }
    }

    private fun checkAnnotationArgumentWithSubElements(
        expression: FirExpression,
        fqName: FqName?,
        session: FirSession,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ): FirDiagnosticFactory0<KtExpression>? {

        fun checkArgumentList(args: FirArgumentList): FirDiagnosticFactory0<KtExpression>? {
            var usedNonConst = false

            for (arg in args.arguments) {
                val sourceForReport = arg.source

                when (val err = checkAnnotationArgumentWithSubElements(arg, fqName, session, reporter, context)) {
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
            is FirArrayOfCall -> return checkArgumentList(expression.argumentList)
            is FirVarargArgumentsExpression -> {
                for (arg in expression.arguments) {
                    val unwrappedArg = if (arg is FirSpreadArgumentExpression) arg.expression else arg
                    checkAnnotationArgumentWithSubElements(unwrappedArg, fqName, session, reporter, context)
                        ?.let { reporter.reportOn(unwrappedArg.source, it, context) }
                }
            }
            else -> {
                val error = when (checkConstantArguments(expression, session)) {
                    ConstantArgumentKind.NOT_CONST -> FirErrors.ANNOTATION_ARGUMENT_MUST_BE_CONST
                    ConstantArgumentKind.ENUM_NOT_CONST -> FirErrors.ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST
                    ConstantArgumentKind.NOT_KCLASS_LITERAL -> FirErrors.ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL
                    ConstantArgumentKind.KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR -> FirErrors.ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR
                    ConstantArgumentKind.NOT_CONST_VAL_IN_CONST_EXPRESSION -> FirErrors.NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION
                    null ->
                        //try to go deeper if we are not sure about this function call
                        //to report non-constant val in not fully resolved calls
                        if (expression is FirFunctionCall) checkArgumentList(expression.argumentList)
                        else null
                }
                if (error != null) {
                    return error
                } else if (annotationFqNamesWithVersion.contains(fqName)) {
                    val argSource = expression.source
                    if (argSource != null) {
                        val stringValue = (expression as? FirConstExpression<*>)?.value as? String
                        if (stringValue != null) {
                            if (!stringValue.matches(RequireKotlinConstants.VERSION_REGEX)) {
                                reporter.reportOn(argSource, FirErrors.ILLEGAL_KOTLIN_VERSION_STRING_VALUE, context)
                            } else if (fqName == sinceKotlinFqName) {
                                val version = ApiVersion.parse(stringValue)
                                val specified = context.session.languageVersionSettings.apiVersion
                                if (version != null && version > specified) {
                                    reporter.report(
                                        FirErrors.NEWER_VERSION_IN_SINCE_KOTLIN.on(argSource, specified.versionString),
                                        context
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    private fun checkDeprecatedSinceKotlin(
        source: FirSourceElement?,
        fqName: FqName?,
        argumentMapping: LinkedHashMap<FirExpression, FirValueParameter>,
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
        for (argument in argumentMapping) {
            val identifier = argument.value.name.identifier
            if (identifier == "warningSince" || identifier == "errorSince" || identifier == "hiddenSince") {
                val argKey = argument.key
                val constExpression = (argKey as? FirConstExpression<*>)
                    ?: ((argKey as? FirNamedArgumentExpression)?.expression as? FirConstExpression<*>)
                val stringValue = constExpression?.value as? String
                if (stringValue != null) {
                    val version = ApiVersion.parse(stringValue)
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
}
