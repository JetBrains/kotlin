/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnsupportedDynamicType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*

object FirTopLevelTypeAliasChecker : FirTypeAliasChecker() {
    override fun check(declaration: FirTypeAlias, context: CheckerContext, reporter: DiagnosticReporter) {
        if (context.containingDeclarations.lastOrNull() !is FirFile) {
            reporter.reportOn(declaration.source, FirErrors.TOPLEVEL_TYPEALIASES_ONLY, context)
        }

        fun containsTypeParameter(type: ConeKotlinType): Boolean {
            val unwrapped = type.lowerBoundIfFlexible().unwrapDefinitelyNotNull()

            if (unwrapped is ConeTypeParameterType) {
                return true
            }

            if (unwrapped is ConeClassLikeType && unwrapped.lookupTag.toSymbol(context.session) is FirTypeAliasSymbol) {
                for (typeArgument in unwrapped.typeArguments) {
                    val typeArgumentType = (typeArgument as? ConeKotlinType) ?: (typeArgument as? ConeKotlinTypeProjection)?.type
                    if (typeArgumentType != null && containsTypeParameter(typeArgumentType)) {
                        return true
                    }
                }
            }

            return false
        }

        val expandedTypeRef = declaration.expandedTypeRef
        val fullyExpandedType = expandedTypeRef.coneType.fullyExpandedType(context.session)

        if (containsTypeParameter(fullyExpandedType) || fullyExpandedType is ConeDynamicType) {
            reporter.reportOn(
                declaration.expandedTypeRef.source,
                FirErrors.TYPEALIAS_SHOULD_EXPAND_TO_CLASS,
                expandedTypeRef.coneType,
                context
            )
        }
    }
}
