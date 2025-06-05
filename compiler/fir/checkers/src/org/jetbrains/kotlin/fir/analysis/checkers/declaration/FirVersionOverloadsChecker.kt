/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isInlineOrValue
import org.jetbrains.kotlin.fir.declarations.utils.isNonLocal
import org.jetbrains.kotlin.fir.declarations.utils.isValue
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

object FirVersionOverloadsChecker : FirFunctionChecker(MppCheckerKind.Common) {
    private val versionArgument = Name.identifier("version")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        // skip checking copy method because it is equivalent to the constructor
        if (declaration.isCopyMethod()) return

        val implicitParameters = declaration.contextParameters + listOfNotNull(declaration.receiverParameter)
        val allParameters = implicitParameters + declaration.valueParameters

        if (allParameters.none { it.hasAnnotation(StandardClassIds.Annotations.IntroducedAt, context.session) }) return

        // check the requirements for a declaration with some @IntroducedAt
        val containingClassSymbol = declaration.getContainingClassSymbol()
        when {
            !declaration.isFinal ->
                reporter.reportOn(declaration.source, FirErrors.INVALID_VERSIONING_ON_NONFINAL_FUNCTION)
            !declaration.isNonLocal ->
                reporter.reportOn(declaration.source, FirErrors.INVALID_VERSIONING_ON_LOCAL_FUNCTION)
            containingClassSymbol?.isFinal == false ->
                reporter.reportOn(declaration.source, FirErrors.INVALID_VERSIONING_ON_NONFINAL_CLASS)
            containingClassSymbol?.classKind == ClassKind.ANNOTATION_CLASS ->
                reporter.reportOn(declaration.source, FirErrors.INVALID_VERSIONING_ON_ANNOTATION_CLASS)
        }

        // check that there are no implicit parameters with it
        for (implicitParameter in implicitParameters) {
            val versionAnnotation = implicitParameter.getAnnotationByClassId(StandardClassIds.Annotations.IntroducedAt, context.session)
            if (versionAnnotation != null) {
                reporter.reportOn(versionAnnotation.source, FirErrors.INVALID_VERSIONING_ON_RECEIVER_OR_CONTEXT_PARAMETER_POSITION)
            }
        }

        var highestVersionUntilNow: MavenComparableVersion? = null
        val paramVersions = mutableMapOf<FirCallableSymbol<*>, MavenComparableVersion>()

        for ((i, param) in declaration.valueParameters.withIndex()) {
            val versionAnnotation = param.getAnnotationByClassId(StandardClassIds.Annotations.IntroducedAt, context.session)
            val isTrailingLambda =
                (i == declaration.valueParameters.lastIndex) && param.returnTypeRef.coneType.isSomeFunctionType(context.session)

            when {
                param.isVararg && (highestVersionUntilNow != null || versionAnnotation != null) ->
                    reporter.reportOn(versionAnnotation?.source ?: param.source, FirErrors.INVALID_VERSIONING_ON_VARARG)

                param.isVal && declaration is FirConstructor && versionAnnotation != null && containingClassSymbol?.isInlineOrValue == true ->
                    reporter.reportOn(versionAnnotation.source, FirErrors.INVALID_VERSIONING_ON_VALUE_CLASS_PARAMETER)

                param.defaultValue == null && versionAnnotation != null ->
                    reporter.reportOn(versionAnnotation.source, FirErrors.INVALID_VERSIONING_ON_NON_OPTIONAL)

                param.defaultValue == null && highestVersionUntilNow != null && !isTrailingLambda ->
                    reporter.reportOn(param.source, FirErrors.INVALID_NON_OPTIONAL_PARAMETER_POSITION)

                versionAnnotation == null -> {}

                else -> {
                    val version =
                        versionAnnotation.getStringArgument(versionArgument, context.session)?.let(::MavenComparableVersion) ?: continue
                    paramVersions[param.symbol] = version

                    if (highestVersionUntilNow lessThanOrEqual version) {
                        highestVersionUntilNow = version
                    } else {
                        reporter.reportOn(versionAnnotation.source, FirErrors.NON_ASCENDING_VERSION_ANNOTATION)
                    }
                }
            }
        }

        val jvmOverloadsAnnotation = declaration.getAnnotationByClassId(StandardClassIds.Annotations.jvmOverloads, context.session)
        if (jvmOverloadsAnnotation != null && paramVersions.isNotEmpty()) {
            reporter.reportOn(jvmOverloadsAnnotation.source, FirErrors.CONFLICT_WITH_JVM_OVERLOADS_ANNOTATION)
        }

        checkDependency(declaration, paramVersions)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDependency(
        declaration: FirFunction,
        paramVersions: Map<FirCallableSymbol<*>, MavenComparableVersion>
    ) {
        val visitor = DependencyChecker(context, reporter, paramVersions)

        for (param in declaration.valueParameters) {
            val defaultValue = param.defaultValue ?: continue
            visitor.check(defaultValue, paramVersions[param.symbol])
        }
    }

    private class DependencyChecker(
        val context: CheckerContext,
        val reporter: DiagnosticReporter,
        val symbolVersions: Map<FirCallableSymbol<*>, MavenComparableVersion>,
    ) : FirDefaultVisitor<Unit, MavenComparableVersion?>() {
        fun check(expression: FirExpression, maxVersion: MavenComparableVersion?) {
            expression.accept(this, maxVersion)
        }

        override fun visitElement(element: FirElement, data: MavenComparableVersion?) {
            element.acceptChildren(this, data)
        }

        override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: MavenComparableVersion?) {
            super.visitQualifiedAccessExpression(qualifiedAccessExpression, data)

            val dependOnSymbol = qualifiedAccessExpression.toResolvedCallableSymbol() ?: return
            val dependVersion = symbolVersions[dependOnSymbol] ?: return

            if (dependVersion greaterThan data) {
                with(context) {
                    reporter.reportOn(
                        qualifiedAccessExpression.source,
                        FirErrors.INVALID_DEFAULT_VALUE_DEPENDENCY,
                        dependOnSymbol,
                        dependVersion.renderString(),
                        data.renderString(),
                    )
                }
            }
        }
    }

    private infix fun MavenComparableVersion?.lessThanOrEqual(other: MavenComparableVersion?): Boolean {
        if (this == null) return true
        if (other == null) return false

        return this <= other
    }

    private infix fun MavenComparableVersion?.greaterThan(other: MavenComparableVersion?): Boolean =
        !this.lessThanOrEqual(other)

    private fun MavenComparableVersion?.renderString() = this?.toString() ?: "base version"

    private fun FirFunction.isCopyMethod(): Boolean =
        nameOrSpecialName == StandardNames.DATA_CLASS_COPY && getContainingClassSymbol()?.isData == true
}