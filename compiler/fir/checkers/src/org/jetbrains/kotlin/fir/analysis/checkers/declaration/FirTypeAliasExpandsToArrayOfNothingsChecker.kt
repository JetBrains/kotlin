/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds

object FirTypeAliasExpandsToArrayOfNothingsChecker : FirTypeAliasChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirTypeAlias, context: CheckerContext, reporter: DiagnosticReporter) {
        val type = declaration.expandedConeType ?: return

        val allowNullableNothing = context.languageVersionSettings.supportsFeature(LanguageFeature.NullableNothingInReifiedPosition)
        if (type.isMalformedExpandedType(context, allowNullableNothing)) {
            reporter.reportOn(declaration.expandedTypeRef.source, FirErrors.TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS, type, context)
        }
    }
}

fun ConeKotlinType.isMalformedExpandedType(context: CheckerContext, allowNullableNothing: Boolean): Boolean {
    val expandedType = fullyExpandedType(context.session)
    if (expandedType.classId == StandardClassIds.Array) {
        val singleArgumentType = expandedType.typeArguments.singleOrNull()?.type?.fullyExpandedType(context.session)
        if (singleArgumentType != null &&
            (singleArgumentType.isNothing || (singleArgumentType.isNullableNothing && !allowNullableNothing))
        ) {
            return true
        }
    }
    return expandedType.containsMalformedArgument(context, allowNullableNothing)
}

private fun ConeKotlinType.containsMalformedArgument(context: CheckerContext, allowNullableNothing: Boolean) =
    typeArguments.any {
        it.type?.fullyExpandedType(context.session)?.isMalformedExpandedType(context, allowNullableNothing) == true
    }
