/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_INTRODUCED_AT_CLASS_ID
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_OVERLOADS_CLASS_ID
import org.jetbrains.kotlin.name.Name

object FirVersionOverloadsChecker : FirFunctionChecker(MppCheckerKind.Common) {
    private val versionNumberArgument = Name.identifier("versionNumber")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        var inVersionedPart = false
        var hasVersionAnnotation = false
        var lastVersionNumber: MavenComparableVersion? = null
        val paramVersions = mutableMapOf<FirCallableSymbol<*>, MavenComparableVersion>()

        for ((i, param) in declaration.valueParameters.withIndex()) {
            val versionAnnotation = param.getAnnotationByClassId(JVM_INTRODUCED_AT_CLASS_ID, context.session)

            if (param.defaultValue == null) {
                val isTrailingLambda = (i == declaration.valueParameters.lastIndex) &&
                        param.returnTypeRef.coneType.isSomeFunctionType(context.session)

                when {
                    inVersionedPart && !isTrailingLambda -> {
                        reporter.reportOn(param.source, FirJvmErrors.INVALID_NON_OPTIONAL_PARAMETER_POSITION)
                    }
                    versionAnnotation != null -> {
                        reporter.reportOn(versionAnnotation.source, FirJvmErrors.INVALID_VERSIONING_ON_NON_OPTIONAL)
                        hasVersionAnnotation = true
                    }
                }

                continue
            }

            if (versionAnnotation == null) continue
            hasVersionAnnotation = true

            inVersionedPart = true
            val versionString = versionAnnotation.getStringArgument(versionNumberArgument, context.session) ?: continue


            val version = MavenComparableVersion(versionString)
            paramVersions[param.symbol] = version

            if (lastVersionNumber.lessThanEqual(version)) {
                lastVersionNumber = version
            } else {
//                reporter.reportOn(param.source, FirJvmErrors.NON_ASCENDING_VERSION_ANNOTATION)
            }
        }

        if (hasVersionAnnotation) {
            when {
                declaration.isOverridable() -> {
                    reporter.reportOn(declaration.source, FirJvmErrors.NONFINAL_VERSIONED_FUNCTION)
                }

                declaration.hasAnnotation(JVM_OVERLOADS_CLASS_ID, context.session) -> {
                    reporter.reportOn(declaration.source, FirJvmErrors.CONFLICT_WITH_JVM_OVERLOADS_ANNOTATION)
                }
            }
        }

        checkDependency(declaration, paramVersions)
    }


    private fun FirFunction.isOverridable(): Boolean = !isFinal || getContainingClass()?.isFinal == false

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

    class DependencyChecker(
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

            val symbol = qualifiedAccessExpression.toResolvedCallableSymbol() ?: return
            val version = symbolVersions[symbol]
            val maxVersion = data

            if (!version.lessThanEqual(maxVersion)) {
                with(context) {
                    reporter.reportOn(qualifiedAccessExpression.source, FirJvmErrors.INVALID_DEFAULT_VALUE_DEPENDENCY)
                }
            }
        }
    }

    private fun MavenComparableVersion?.lessThanEqual(other: MavenComparableVersion?): Boolean {
        if (this == null) return true
        if (other == null) return false

        return this <= other
    }
}