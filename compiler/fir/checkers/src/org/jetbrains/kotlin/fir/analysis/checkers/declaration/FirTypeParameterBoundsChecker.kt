/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirRealSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.getAncestors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeCheckerProviderContext
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirTypeParameterBoundsChecker : FirTypeParameterChecker() {

    private val classKinds = setOf(
        ClassKind.CLASS,
        ClassKind.ENUM_CLASS,
        ClassKind.OBJECT
    )

    override fun check(declaration: FirTypeParameter, context: CheckerContext, reporter: DiagnosticReporter) {
        val containingDeclaration = context.containingDeclarations.lastOrNull() ?: return
        if (containingDeclaration is FirConstructor) return

        checkFinalUpperBounds(declaration, containingDeclaration, context, reporter)
        checkExtensionFunctionTypeBound(declaration, context, reporter)

        if (containingDeclaration.safeAs<FirMemberDeclaration>()?.isInlineOnly() != true) {
            checkOnlyOneTypeParameterBound(declaration, context, reporter)
        }

        checkBoundUniqueness(declaration, context, reporter)
        checkConflictingBounds(declaration, context, reporter)
        checkTypeAliasBound(declaration, containingDeclaration, context, reporter)
        checkBoundsPlacement(declaration, context, reporter)
        checkDynamicBounds(declaration, context, reporter)
    }

    private fun checkFinalUpperBounds(
        declaration: FirTypeParameter,
        containingDeclaration: FirDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (containingDeclaration is FirSimpleFunction && containingDeclaration.isOverride) return
        if (containingDeclaration is FirProperty && containingDeclaration.isOverride) return

        declaration.symbol.fir.bounds.forEach { bound ->
            if (!bound.coneType.canHaveSubtypes(context.session)) {
                reporter.reportOn(bound.source, FirErrors.FINAL_UPPER_BOUND, bound.coneType, context)
            }
        }
    }

    private fun checkExtensionFunctionTypeBound(declaration: FirTypeParameter, context: CheckerContext, reporter: DiagnosticReporter) {
        declaration.symbol.fir.bounds.forEach { bound ->
            if (bound.isExtensionFunctionType(context.session)) {
                reporter.reportOn(bound.source, FirErrors.UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE, context)
            }
        }
    }

    private fun checkTypeAliasBound(
        declaration: FirTypeParameter,
        containingDeclaration: FirDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (containingDeclaration is FirTypeAlias) {
            declaration.bounds.filter { it.source?.kind == FirRealSourceElementKind }.forEach { bound ->
                reporter.reportOn(bound.source, FirErrors.BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED, context)
            }
        }
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
        return source.treeStructure.getAncestors(source.lighterASTNode)
            .find { it.tokenType == KtNodeTypes.TYPE_CONSTRAINT || it.tokenType == KtNodeTypes.TYPE_PARAMETER }
            ?.tokenType == KtNodeTypes.TYPE_CONSTRAINT
    }


    private fun checkBoundUniqueness(declaration: FirTypeParameter, context: CheckerContext, reporter: DiagnosticReporter) {
        val seenClasses = mutableSetOf<FirRegularClass>()
        val allNonErrorBounds = declaration.bounds.filter { it !is FirErrorTypeRef }
        val uniqueBounds = allNonErrorBounds.distinctBy { it.coneType.classId ?: it.coneType }

        uniqueBounds.forEach { bound ->
            bound.coneType.toRegularClass(context.session)?.let { clazz ->
                if (classKinds.contains(clazz.classKind) && seenClasses.add(clazz) && seenClasses.size > 1) {
                    reporter.reportOn(bound.source, FirErrors.ONLY_ONE_CLASS_BOUND_ALLOWED, context)
                }
            }
        }

        allNonErrorBounds.minus(uniqueBounds).forEach { bound ->
            reporter.reportOn(bound.source, FirErrors.REPEATED_BOUND, context)
        }
    }

    private fun checkConflictingBounds(declaration: FirTypeParameter, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.bounds.size < 2) return

        fun anyConflictingTypes(types: List<ConeKotlinType>): Boolean {
            types.forEach { type ->
                if (!type.canHaveSubtypes(context.session)) {
                    types.forEach { otherType ->
                        if (type != otherType && !type.isRelated(context.session.typeContext, otherType)) {
                            return true
                        }
                    }
                }
            }
            return false
        }

        if (anyConflictingTypes(declaration.bounds.map { it.coneType })) {
            reporter.reportOn(declaration.source, FirErrors.CONFLICTING_UPPER_BOUNDS, declaration.symbol, context)
        }
    }

    //TODO should be moved to extended checkers (because this is basically a code-style warning)
    private fun checkBoundsPlacement(declaration: FirTypeParameter, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.bounds.size < 2) return

        val (constraint, params) = declaration.bounds.partition { it.isInTypeConstraint() }
        if (params.isNotEmpty() && constraint.isNotEmpty()) {
            reporter.reportOn(declaration.source, FirErrors.MISPLACED_TYPE_PARAMETER_CONSTRAINTS, context)
        }
    }

    private fun checkDynamicBounds(declaration: FirTypeParameter, context: CheckerContext, reporter: DiagnosticReporter) {
        declaration.bounds.forEach { bound ->
            if (bound is FirDynamicTypeRef) {
                reporter.reportOn(bound.source, FirErrors.DYNAMIC_UPPER_BOUND, context)
            }
        }
    }

    private fun KotlinTypeMarker.isRelated(context: TypeCheckerProviderContext, type: KotlinTypeMarker?): Boolean =
        isSubtypeOf(context, type) || isSupertypeOf(context, type)


}
