/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.FE10LikeConeSubstitutor
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkUpperBoundViolated
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toTypeArgumentsWithSourceInfo
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.unwrapSmartcastExpression
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.classTypeParameterSymbols
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeOuterClassArgumentsRequired
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConePlaceholderProjectionInQualifierResolution
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeWrongNumberOfTypeArgumentsError
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

object FirTypeArgumentsOfQualifierOfCallableReferenceChecker : FirCallableReferenceAccessChecker(MppCheckerKind.Common) {

    context(context: CheckerContext)
    private val innerClassesProperlySupported: Boolean
        get() = LanguageFeature.ProperSupportOfInnerClassesInCallableReferenceLHS.isEnabled()

    /**
     * @return true if **error** was reported
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkNonFatalDiagnostics(
        expression: FirCallableReferenceAccess,
        lhs: FirResolvedQualifier,
        classSymbol: FirClassLikeSymbol<*>,
        lhsType: ConeKotlinType,
    ): Boolean {
        for (diagnostic in expression.nonFatalDiagnostics) {
            when (diagnostic) {
                is ConeWrongNumberOfTypeArgumentsError -> {
                    val positioning =
                        if (diagnostic.isDeprecationErrorForCallableReferenceLHS) {
                            SourceElementPositioningStrategies.TYPE_ARGUMENT_LIST_OR_WITHOUT_RECEIVER
                        } else {
                            // here, `desiredCount` corresponds to the number of type parameters for all parts of the qualifier altogether,
                            // hence reporting on the whole qualifier
                            SourceElementPositioningStrategies.DEFAULT
                        }

                    if (diagnostic.isDeprecationErrorForCallableReferenceLHS && !innerClassesProperlySupported) {
                        if (classSymbol.isLocal) {
                            reporter.reportOn(
                                diagnostic.source,
                                FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_LOCAL_CLASS_IN_LHS_WARNING,
                                diagnostic.desiredCount,
                                diagnostic.symbol,
                                positioningStrategy = positioning,
                            )
                        } else {
                            reporter.reportOn(
                                diagnostic.source,
                                FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS_WARNING,
                                diagnostic.desiredCount,
                                diagnostic.symbol,
                                lhsType,
                                positioningStrategy = positioning,
                            )
                        }
                        continue
                    }

                    reporter.reportOn(
                        diagnostic.source,
                        FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS,
                        diagnostic.desiredCount,
                        diagnostic.symbol,
                        positioningStrategy = positioning,
                    )
                    return true
                }
                is ConeOuterClassArgumentsRequired -> {
                    reporter.reportOn(
                        lhs.source,
                        FirErrors.OUTER_CLASS_ARGUMENTS_REQUIRED,
                        diagnostic.symbol,
                    )
                    return true
                }
            }
        }
        return false
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCallableReferenceAccess) {
        val lhs = expression.explicitReceiver?.unwrapSmartcastExpression() as? FirResolvedQualifier ?: return
        val correspondingDeclaration = lhs.symbol ?: return
        val lhsType = lhs.resolvedLHSTypeForCallableReferenceOrNull ?: return

        for (argument in lhs.typeArguments) {
            val errorTypeRef = (argument as? FirTypeProjectionWithVariance)?.typeRef as? FirErrorTypeRef ?: continue

            if (errorTypeRef.diagnostic is ConePlaceholderProjectionInQualifierResolution) {
                reporter.reportOn(argument.source, FirErrors.PLACEHOLDER_PROJECTION_IN_QUALIFIER)
            }
        }

        if (checkNonFatalDiagnostics(expression, lhs, correspondingDeclaration, lhsType)) return

        val typeArgumentsWithSourceInfo = lhs.typeArguments.toTypeArgumentsWithSourceInfo()
        var typeArguments = when {
            innerClassesProperlySupported -> {
                // We prefer explicit type arguments as we have source info for them
                List(lhsType.typeArguments.size) { index ->
                    if (index < typeArgumentsWithSourceInfo.size) typeArgumentsWithSourceInfo[index]
                    else lhsType.typeArguments[index]
                }
            }
            else -> {
                typeArgumentsWithSourceInfo
            }
        }
        var typeParameterSymbols = when {
            innerClassesProperlySupported -> {
                correspondingDeclaration.typeParameterSymbols
            }
            else -> {
                correspondingDeclaration.classTypeParameterSymbols
            }
        }

        if (correspondingDeclaration is FirTypeAliasSymbol) {
            val qualifierType = correspondingDeclaration.constructType(typeArgumentsWithSourceInfo.toTypedArray())
            val expandedLhsType = qualifierType.fullyExpandedType()
            typeArguments = expandedLhsType.typeArguments.toList()

            val expandedClassSymbol = correspondingDeclaration.resolvedExpandedTypeRef.toRegularClassSymbol(context.session) ?: return
            typeParameterSymbols = expandedClassSymbol.typeParameterSymbols
        }

        val substitutor = FE10LikeConeSubstitutor(typeParameterSymbols, typeArguments, context.session)
        checkUpperBoundViolated(
            typeParameterSymbols,
            typeArguments,
            substitutor,
            // Manipulations with `constructType()` and `fullyExpandedType()` above may shove
            // the argument with the true source element arbitrarily deep, so we may end up
            // with "source must not be null".
            fallbackSource = lhs.source,
            isTypealiasExpansion = correspondingDeclaration is FirTypeAliasSymbol,
        )
    }
}
