/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.extractArgumentTypeRefAndSource
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirTypedDeclaration
import org.jetbrains.kotlin.fir.resolve.getClassThatContainsTypeParameter
import org.jetbrains.kotlin.fir.resolve.isValidTypeParameter
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.toTypeProjections
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*

object FirOuterClassArgumentsRequiredChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        for (superTypeRef in declaration.superTypeRefs) {
            checkOuterClassArgumentsRequired(superTypeRef, declaration, context, reporter)
        }

        for (subDecl in declaration.declarations) {
            if (subDecl is FirTypedDeclaration) {
                checkOuterClassArgumentsRequired(subDecl.returnTypeRef, declaration, context, reporter)
            }
        }
    }
}

private fun checkOuterClassArgumentsRequired(
    typeRef: FirTypeRef,
    declaration: FirRegularClass?,
    context: CheckerContext,
    reporter: DiagnosticReporter
) {
    val type: ConeKotlinType

    if (typeRef is FirResolvedTypeRef) {
        if (typeRef is FirErrorTypeRef) {
            return
        }

        type = typeRef.type
        val delegatedTypeRef = typeRef.delegatedTypeRef

        if (delegatedTypeRef is FirUserTypeRef) {

            if (type is ConeClassLikeType) {
                val symbol = type.lookupTag.toSymbol(context.session)

                if (symbol is FirRegularClassSymbol) {
                    var problemTypeParameter: FirTypeParameterRef? = null
                    val typeArguments = delegatedTypeRef.qualifier.toTypeProjections()
                    val argumentsFromOuterClassesAndParentsCount = symbol.fir.typeParameters.drop(typeArguments.size).sumOf {
                        val result = if (isValidTypeParameter(it, declaration, context.session)) {
                            1
                        } else {
                            if (problemTypeParameter == null) {
                                problemTypeParameter = it
                            }
                            0
                        }
                        return@sumOf result
                    }
                    val finalTypeArgumentsCount = typeArguments.size + argumentsFromOuterClassesAndParentsCount

                    if (finalTypeArgumentsCount != symbol.fir.typeParameters.size) {
                        val source = typeRef.source
                        if (problemTypeParameter != null) {
                            var outerClass: FirRegularClass? = null
                            context.findClosest<FirRegularClass> {
                                outerClass = getClassThatContainsTypeParameter(it, problemTypeParameter!!)
                                return@findClosest outerClass != null
                            }
                            if (outerClass != null) {
                                reporter.reportOn(source, FirErrors.OUTER_CLASS_ARGUMENTS_REQUIRED, outerClass!!, context)
                            }
                        } else {
                            reporter.reportOn(
                                source,
                                FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS,
                                symbol.fir.typeParameters.size - argumentsFromOuterClassesAndParentsCount,
                                symbol,
                                context
                            )
                        }
                    }
                }
            }
        }
    } else if (typeRef is ConeKotlinType) {
        type = typeRef
    } else {
        return
    }

    for (index in type.typeArguments.indices) {
        val firTypeRefSource = extractArgumentTypeRefAndSource(typeRef, index) ?: continue
        firTypeRefSource.typeRef?.let { checkOuterClassArgumentsRequired(it, declaration, context, reporter) }
    }
}