/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

object FirVersionOverloadsChecker : FirFunctionChecker(MppCheckerKind.Common) {
    private val versionArgument = Name.identifier("version")
    private val copyMethodName = Name.identifier("copy")
    private val JvmOverloadsClassId =  ClassId.topLevel(FqName("kotlin.jvm.JvmOverloads"))

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        // skip checking copy method because it is equivalent to the constructor
        if (declaration.isCopyMethod()) return

        val inOverridableFunction = declaration.isOverridable()
        var inVersionedPart = false
        var lastVersionNumber: MavenComparableVersion? = null
        val paramVersions = mutableMapOf<FirCallableSymbol<*>, MavenComparableVersion>()

        for ((i, param) in declaration.valueParameters.withIndex()) {
            val versionAnnotation = param.getAnnotationByClassId(StandardClassIds.Annotations.IntroducedAt, context.session)

            if (param.defaultValue == null) {
                val isTrailingLambda = (i == declaration.valueParameters.lastIndex) &&
                        param.returnTypeRef.coneType.isSomeFunctionType(context.session)

                when {
                    versionAnnotation != null ->
                        reporter.reportOn(versionAnnotation.source, FirErrors.INVALID_VERSIONING_ON_NON_OPTIONAL)

                    inVersionedPart && !isTrailingLambda ->
                        reporter.reportOn(param.source, FirErrors.INVALID_NON_OPTIONAL_PARAMETER_POSITION)
                }

                continue
            }

            if (versionAnnotation == null) continue

            inVersionedPart = true
            val versionString = versionAnnotation.getStringArgument(versionArgument, context.session) ?: continue

            if (inOverridableFunction) reporter.reportOn(versionAnnotation.source, FirErrors.NONFINAL_VERSIONED_FUNCTION)

            val version = MavenComparableVersion(versionString)
            paramVersions[param.symbol] = version

            if (lastVersionNumber.lessThanEqual(version)) {
                lastVersionNumber = version
            } else {
                reporter.reportOn(versionAnnotation.source, FirErrors.NON_ASCENDING_VERSION_ANNOTATION)
            }
        }

        val jvmOverloadAnnotation = declaration.getAnnotationByClassId(JvmOverloadsClassId, context.session)
        if (jvmOverloadAnnotation != null && paramVersions.isNotEmpty()) {
            reporter.reportOn(jvmOverloadAnnotation.source, FirErrors.CONFLICT_WITH_JVM_OVERLOADS_ANNOTATION)
        }

        checkDependency(declaration, paramVersions)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDependency(
        declaration: FirFunction,
        paramVersions: Map<FirCallableSymbol<*>, MavenComparableVersion>
    ) {
        val visitor = DependencyChecker(context, reporter,paramVersions)

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

            if (!dependVersion.lessThanEqual(data)) {
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

    private fun MavenComparableVersion?.lessThanEqual(other: MavenComparableVersion?): Boolean {
        if (this == null) return true
        if (other == null) return false

        return this <= other
    }

    private fun MavenComparableVersion?.renderString() = this?.toString() ?: "base version"

    private fun FirFunction.isOverridable(): Boolean = !isFinal || getContainingClassSymbol()?.isFinal == false

    private fun FirFunction.isCopyMethod(): Boolean = nameOrSpecialName == copyMethodName && getContainingClassSymbol()?.isData == true
}