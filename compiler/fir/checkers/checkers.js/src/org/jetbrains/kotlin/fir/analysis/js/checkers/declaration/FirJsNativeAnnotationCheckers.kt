/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.isNativeObject
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JsStandardClassIds

internal abstract class FirJsAbstractNativeAnnotationChecker(private val requiredAnnotation: ClassId) : FirSimpleFunctionChecker(
    MppCheckerKind.Common) {
    protected fun FirFunction.hasRequiredAnnotation(context: CheckerContext) = hasAnnotation(requiredAnnotation, context.session)

    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotation = declaration.getAnnotationByClassId(requiredAnnotation, context.session) ?: return

        val isMember = !context.isTopLevel && declaration.visibility != Visibilities.Local
        val isExtension = declaration.isExtension

        if (isMember && (isExtension || !declaration.symbol.isNativeObject(context)) || !isMember && !isExtension) {
            reporter.reportOn(
                declaration.source,
                FirJsErrors.NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN,
                annotation.resolvedType,
                context
            )
        }
    }
}

internal object FirJsNativeInvokeChecker : FirJsAbstractNativeAnnotationChecker(JsStandardClassIds.Annotations.JsNativeInvoke)

internal abstract class FirJsAbstractNativeIndexerChecker(
    requiredAnnotation: ClassId,
    private val indexerKind: String,
    private val requiredParametersCount: Int,
) : FirJsAbstractNativeAnnotationChecker(requiredAnnotation) {
    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        super.check(declaration, context, reporter)

        val parameters = declaration.valueParameters
        val builtIns = context.session.builtinTypes

        if (parameters.isNotEmpty()) {
            val firstParameterDeclaration = parameters.first()
            val firstParameter = firstParameterDeclaration.returnTypeRef.coneType

            if (
                firstParameter !is ConeErrorType &&
                !firstParameter.isString &&
                !firstParameter.isSubtypeOf(builtIns.numberType.coneType, context.session)
            ) {
                reporter.reportOn(
                    firstParameterDeclaration.source,
                    FirJsErrors.NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER,
                    indexerKind,
                    context
                )
            }
        }

        if (parameters.size != requiredParametersCount) {
            reporter.reportOn(
                declaration.source,
                FirJsErrors.NATIVE_INDEXER_WRONG_PARAMETER_COUNT,
                requiredParametersCount,
                indexerKind,
                context
            )
        }

        for (parameter in parameters) {
            if (parameter.defaultValue != null) {
                reporter.reportOn(
                    parameter.source,
                    FirJsErrors.NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS,
                    indexerKind,
                    context
                )
            }
        }
    }
}

internal object FirJsNativeGetterChecker : FirJsAbstractNativeIndexerChecker(JsStandardClassIds.Annotations.JsNativeGetter, "getter", 1) {
    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.hasRequiredAnnotation(context)) return
        super.check(declaration, context, reporter)

        if (!declaration.returnTypeRef.coneType.isNullable) {
            reporter.reportOn(declaration.source, FirJsErrors.NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE, context)
        }
    }
}

internal object FirJsNativeSetterChecker : FirJsAbstractNativeIndexerChecker(JsStandardClassIds.Annotations.JsNativeSetter, "setter", 2) {
    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.hasRequiredAnnotation(context)) return
        super.check(declaration, context, reporter)

        val returnType = declaration.returnTypeRef.coneType
        if (returnType.isUnit) {
            return
        }

        if (declaration.valueParameters.size < 2) {
            return
        }

        val secondParameterType = declaration.valueParameters[1].returnTypeRef.coneType
        if (secondParameterType.isSubtypeOf(returnType, context.session)) {
            return
        }

        reporter.reportOn(declaration.source, FirJsErrors.NATIVE_SETTER_WRONG_RETURN_TYPE, context)
    }
}
