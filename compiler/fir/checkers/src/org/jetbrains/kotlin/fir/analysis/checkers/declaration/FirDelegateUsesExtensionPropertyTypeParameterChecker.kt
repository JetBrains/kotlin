/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toTypeParameterSymbol
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*

object FirDelegateUsesExtensionPropertyTypeParameterChecker : FirPropertyChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        if (!declaration.isExtension) return
        val delegate = declaration.delegate ?: return
        val parameters = declaration.typeParameters.mapTo(hashSetOf()) { it.symbol }

        val usedTypeParameterSymbol = delegate.resolvedType.findUsedTypeParameterSymbol(parameters, delegate)
            ?: return

        reporter.reportOn(declaration.source, FirErrors.DELEGATE_USES_EXTENSION_PROPERTY_TYPE_PARAMETER_ERROR, usedTypeParameterSymbol)
    }

    context(context: CheckerContext)
    private fun ConeKotlinType.findUsedTypeParameterSymbol(
        typeParameterSymbols: HashSet<FirTypeParameterSymbol>,
        delegate: FirExpression,
    ): FirTypeParameterSymbol? {
        val expandedDelegateClassLikeType =
            delegate.resolvedType.unwrapToSimpleTypeUsingLowerBound().fullyExpandedType() as? ConeClassLikeType
                ?: return null
        val delegateClassSymbol = expandedDelegateClassLikeType.lookupTag.toClassSymbol() ?: return null
        val delegateClassScope by lazy(LazyThreadSafetyMode.NONE) { delegateClassSymbol.unsubstitutedScope() }
        for (it in typeArguments) {
            val theType = it.type ?: continue
            val argumentAsTypeParameterSymbol = theType.toTypeParameterSymbol()

            if (argumentAsTypeParameterSymbol in typeParameterSymbols) {
                var propertyWithTypeParameterTypeFound = false
                delegateClassScope.processAllProperties { symbol ->
                    if (symbol.resolvedReturnType.contains { it is ConeTypeParameterType }) {
                        propertyWithTypeParameterTypeFound = true
                    }
                }
                if (propertyWithTypeParameterTypeFound) {
                    return argumentAsTypeParameterSymbol
                }
            }
            val usedTypeParameterSymbol = theType.findUsedTypeParameterSymbol(typeParameterSymbols, delegate)
            if (usedTypeParameterSymbol != null) {
                return usedTypeParameterSymbol
            }
        }

        return null
    }
}
