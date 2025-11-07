/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isInlineOrValue
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

object FirVersionOverloadsChecker : FirFunctionChecker(MppCheckerKind.Platform) {
    private val versionArgument = Name.identifier("version")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        // skip the 'copy' method because it is equivalent to the constructor
        if (declaration.isCopyMethod()) return

        // check that there are no implicit parameters with it
        var contextWithImplicitParameter = false
        for (implicitParameter in declaration.contextParameters + listOfNotNull(declaration.receiverParameter)) {
            val versionAnnotation = implicitParameter.getIntroducedAtAnnotation()
            if (versionAnnotation != null) {
                reporter.reportOn(versionAnnotation.source, FirErrors.INVALID_VERSIONING_ON_RECEIVER_OR_CONTEXT_PARAMETER_POSITION)
                contextWithImplicitParameter = true
            }
        }

        // no @IntroducedAt annotations at all, nothing to do
        if (!contextWithImplicitParameter && declaration.valueParameters.none { it.hasIntroducedAtAnnotation() }) return

        checkDeclarationOrContainingClass(declaration)
        val paramVersions = computeAndCheckParameterVersions(declaration)

        val dependencyChecker = DependencyChecker(context, reporter, paramVersions)
        val complexExpressionChecker = ComplexExpressionChecker(context, reporter)
        for (param in declaration.valueParameters) {
            val defaultValue = param.defaultValue ?: continue
            defaultValue.accept(dependencyChecker, paramVersions[param.symbol])
            defaultValue.accept(complexExpressionChecker)
        }
    }

    context(context: CheckerContext)
    private fun FirDeclaration.hasIntroducedAtAnnotation(): Boolean =
        hasAnnotation(StandardClassIds.Annotations.IntroducedAt, context.session)

    context(context: CheckerContext)
    private fun FirDeclaration.getIntroducedAtAnnotation(): FirAnnotation? =
        getAnnotationByClassId(StandardClassIds.Annotations.IntroducedAt, context.session)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDeclarationOrContainingClass(declaration: FirFunction): Boolean {
        // check the requirements for a declaration with some @IntroducedAt
        val containingClassSymbol = declaration.getContainingClassSymbol()
        when {
            !declaration.isFinal ->
                reporter.reportOn(
                    declaration.source,
                    FirErrors.INVALID_VERSIONING_ON_NONFINAL_FUNCTION,
                    SourceElementPositioningStrategies.DECLARATION_NAME
                )
            declaration.isLocal ->
                reporter.reportOn(
                    declaration.source,
                    FirErrors.INVALID_VERSIONING_ON_LOCAL_FUNCTION,
                    SourceElementPositioningStrategies.DECLARATION_NAME
                )
            declaration !is FirConstructor && containingClassSymbol?.isFinal == false ->
                reporter.reportOn(
                    declaration.source,
                    FirErrors.INVALID_VERSIONING_ON_NONFINAL_CLASS,
                    SourceElementPositioningStrategies.DECLARATION_NAME
                )
            containingClassSymbol?.classKind == ClassKind.ANNOTATION_CLASS ->
                reporter.reportOn(
                    declaration.source,
                    FirErrors.INVALID_VERSIONING_ON_ANNOTATION_CLASS,
                    SourceElementPositioningStrategies.DECLARATION_NAME
                )
            else -> return false
        }
        // if we reach this point, there were problems with the declaration or its containing class
        return true
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun computeAndCheckParameterVersions(declaration: FirFunction): Map<FirCallableSymbol<*>, MavenComparableVersion> {
        val containingClassSymbol = declaration.getContainingClassSymbol()

        var highestVersionUntilNow: MavenComparableVersion? = null
        val paramVersions = mutableMapOf<FirCallableSymbol<*>, MavenComparableVersion>()

        for ((i, param) in declaration.valueParameters.withIndex()) {
            val versionAnnotation = param.getAnnotationByClassId(StandardClassIds.Annotations.IntroducedAt, context.session)
            val version = versionAnnotation?.getStringArgument(versionArgument, context.session)?.let(::MavenComparableVersion)

            // after one @IntroducedAt, only trailing lambdas may not be optional
            if (version == null) {
                val mayBeTrailingLambda =
                    (i == declaration.valueParameters.lastIndex) && param.returnTypeRef.coneType.isSomeFunctionType(context.session)
                if (param.defaultValue == null && highestVersionUntilNow != null && !mayBeTrailingLambda) {
                    reporter.reportOn(param.source, FirErrors.INVALID_NON_OPTIONAL_PARAMETER_POSITION)
                }
                continue
            }

            // update version map and check arguments
            paramVersions[param.symbol] = version

            when {
                param.isVararg ->
                    reporter.reportOn(versionAnnotation.source ?: param.source, FirErrors.INVALID_VERSIONING_ON_VARARG)

                param.isVal && declaration is FirConstructor && containingClassSymbol?.isInlineOrValue == true ->
                    reporter.reportOn(versionAnnotation.source, FirErrors.INVALID_VERSIONING_ON_VALUE_CLASS_PARAMETER)

                param.defaultValue == null ->
                    reporter.reportOn(versionAnnotation.source, FirErrors.INVALID_VERSIONING_ON_NON_OPTIONAL)
            }

            if (highestVersionUntilNow lessThanOrEqual version) {
                highestVersionUntilNow = version
            } else {
                reporter.reportOn(
                    versionAnnotation.source,
                    FirErrors.NON_ASCENDING_VERSION_ANNOTATION,
                    version,
                    highestVersionUntilNow,
                    paramVersions.firstNotNullOf { (symbol, version) ->
                        symbol.takeIf { version == highestVersionUntilNow }
                    }
                )
            }
        }

        return paramVersions
    }

    private class DependencyChecker(
        val context: CheckerContext,
        val reporter: DiagnosticReporter,
        val symbolVersions: Map<FirCallableSymbol<*>, MavenComparableVersion>,
    ) : FirDefaultVisitor<Unit, MavenComparableVersion?>() {
        override fun visitElement(element: FirElement, data: MavenComparableVersion?) {
            element.acceptChildren(this, data)
        }

        override fun visitQualifiedAccessExpression(
            qualifiedAccessExpression: FirQualifiedAccessExpression,
            data: MavenComparableVersion?
        ) {
            val dependOnSymbol = qualifiedAccessExpression.toResolvedCallableSymbol() ?: return
            val dependVersion = symbolVersions[dependOnSymbol]

            if (dependVersion greaterThan data) {
                with(context) {
                    reporter.reportOn(
                        qualifiedAccessExpression.source,
                        FirErrors.INVALID_DEFAULT_VALUE_DEPENDENCY,
                        data,
                        dependVersion,
                    )
                }
            }
        }
    }

    private class ComplexExpressionChecker(
        val context: CheckerContext,
        val reporter: DiagnosticReporter
    ) : FirDefaultVisitorVoid() {
        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression) {
            anonymousObjectExpression.tooComplex()
        }

        fun FirElement.tooComplex() {
            reporter.reportOn(source, FirErrors.VERSION_OVERLOADS_TOO_COMPLEX_EXPRESSION, context)
        }
    }

    private infix fun MavenComparableVersion?.lessThanOrEqual(other: MavenComparableVersion?): Boolean {
        if (this == null) return true
        if (other == null) return false

        return this <= other
    }

    private infix fun MavenComparableVersion?.greaterThan(other: MavenComparableVersion?): Boolean =
        !this.lessThanOrEqual(other)

    private fun FirFunction.isCopyMethod(): Boolean =
        origin == FirDeclarationOrigin.Synthetic.DataClassMember
                && nameOrSpecialName == StandardNames.DATA_CLASS_COPY
                && getContainingClassSymbol()?.isData == true
}