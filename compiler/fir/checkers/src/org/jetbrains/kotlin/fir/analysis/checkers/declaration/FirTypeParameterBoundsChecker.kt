/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.analysis.checkers.canHaveSubtypes
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isInlineOnly
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isExtensionFunctionType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirTypeParameterBoundsChecker : FirTypeParameterChecker() {

    private val classKinds = setOf(
        ClassKind.CLASS,
        ClassKind.ENUM_CLASS,
        ClassKind.OBJECT
    )

    override fun check(declaration: FirTypeParameter, context: CheckerContext, reporter: DiagnosticReporter) {
        val containingDeclaration = context.containingDeclarations.lastOrNull()
        if (containingDeclaration is FirConstructor) return
        if (containingDeclaration is FirSimpleFunction && containingDeclaration.isOverride) return
        if (containingDeclaration is FirProperty && containingDeclaration.isOverride) return

        declaration.symbol.fir.bounds.forEach { bound ->
            if (!bound.coneType.canHaveSubtypes(context.session)) {
                reporter.reportOn(bound.source, FirErrors.FINAL_UPPER_BOUND, bound.coneType, context)
            }
            if (bound.isExtensionFunctionType(context.session)) {
                reporter.reportOn(bound.source, FirErrors.UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE, context)
            }
        }

        if (containingDeclaration.safeAs<FirMemberDeclaration>()?.isInlineOnly() != true) {
            checkOnlyOneTypeParameterBound(declaration, context, reporter)
        }

        checkOnlyOneClassBound(declaration, context, reporter)
    }

    private fun checkOnlyOneTypeParameterBound(declaration: FirTypeParameter, context: CheckerContext, reporter: DiagnosticReporter) {
        val bounds = declaration.bounds.distinctBy { it.coneType }
        val (boundWithParam, otherBounds) = bounds.partition { it.coneType is ConeTypeParameterType }
        if (boundWithParam.size > 1 || (boundWithParam.size == 1 && otherBounds.isNotEmpty())) {
            // If there's only one problematic bound (either 2 type parameter bounds, or 1 type parameter bound + 1 other bound),
            // report the diagnostic on that bound

            //take TypeConstraint bounds only to report on the same point as old FE
            val constraintBounds = bounds.filter { it.isInTypeConstraint() }.toSet()
            val reportOn =
                if (bounds.size == 2) {
                    val boundDecl = otherBounds.firstOrNull() ?: boundWithParam.last()
                    if (constraintBounds.contains(boundDecl)) boundDecl.source
                    else declaration.source
                } else {
                    declaration.source
                }
            reporter.reportOn(reportOn, FirErrors.BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER, context)
        }
    }

    private fun FirTypeRef.isInTypeConstraint(): Boolean {
        val source = source ?: return false
        return source.treeStructure.getParent(source.lighterASTNode)?.tokenType == KtNodeTypes.TYPE_CONSTRAINT
    }


    private fun checkOnlyOneClassBound(declaration: FirTypeParameter, context: CheckerContext, reporter: DiagnosticReporter) {
        val seenClasses = mutableSetOf<FirRegularClass>()
        val bounds = declaration.bounds.distinctBy { it.coneType }
        bounds.forEach { bound ->
            bound.coneType.toRegularClass(context.session)?.let { clazz ->
                if (classKinds.contains(clazz.classKind) && seenClasses.add(clazz) && seenClasses.size > 1) {
                    reporter.reportOn(bound.source, FirErrors.ONLY_ONE_CLASS_BOUND_ALLOWED, context)
                }
            }
        }
    }
}
