/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.impl.deduplicating
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.scopes.impl.delegatedWrapperData
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeCheckerContext
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.types.AbstractTypeChecker

object FirImplementationMismatchChecker : FirClassChecker() {

    override fun check(declaration: FirClass<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = declaration.source ?: return
        val sourceKind = source.kind
        if (sourceKind is FirFakeSourceElementKind && sourceKind != FirFakeSourceElementKind.EnumInitializer) return
        if (declaration is FirRegularClass && declaration.isExpect) return
        val classKind = declaration.classKind
        if (classKind == ClassKind.ANNOTATION_CLASS || classKind == ClassKind.ENUM_CLASS) return

        val typeCheckerContext = context.session.typeContext.newBaseTypeCheckerContext(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )
        val classScope = declaration.unsubstitutedScope(context)
        val dedupReporter = reporter.deduplicating()

        for (name in classScope.getCallableNames()) {
            classScope.processFunctionsByName(name) { checkInheritanceClash(declaration, context, dedupReporter, typeCheckerContext, it) }
            classScope.processPropertiesByName(name) {
                checkInheritanceClash(declaration, context, dedupReporter, typeCheckerContext, it)
                checkValOverrideVar(declaration, context, dedupReporter, it)
            }
        }
    }

    private fun checkInheritanceClash(
        containingClass: FirClass<*>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        typeCheckerContext: ConeTypeCheckerContext,
        symbol: FirCallableSymbol<*>
    ) {
        fun reportTypeMismatch(member1: FirCallableDeclaration<*>, member2: FirCallableDeclaration<*>, isDelegation: Boolean) {
            val error = if (member1 is FirProperty && member2 is FirProperty) {
                if (member1.isVar || member2.isVar) {
                    FirErrors.VAR_TYPE_MISMATCH_ON_INHERITANCE
                } else {
                    if (isDelegation) FirErrors.PROPERTY_TYPE_MISMATCH_BY_DELEGATION
                    else FirErrors.PROPERTY_TYPE_MISMATCH_ON_INHERITANCE
                }
            } else {
                if (isDelegation) FirErrors.RETURN_TYPE_MISMATCH_BY_DELEGATION
                else FirErrors.RETURN_TYPE_MISMATCH_ON_INHERITANCE
            }
            reporter.reportOn(containingClass.source, error, member1, member2, context)
        }

        fun canOverride(
            baseMember: FirCallableDeclaration<*>,
            inheritedType: ConeKotlinType,
            baseType: ConeKotlinType
        ): Boolean =
            if (baseMember is FirProperty && baseMember.isVar) AbstractTypeChecker.equalTypes(typeCheckerContext, inheritedType, baseType)
            else AbstractTypeChecker.isSubtypeOf(typeCheckerContext, inheritedType, baseType)


        if (symbol.callableId.classId != containingClass.classId) return
        if (symbol !is FirIntersectionCallableSymbol) return
        val withTypes = symbol.intersections.map {
            it.fir to context.returnTypeCalculator.tryCalculateReturnType(it.fir).coneType
        }

        if (withTypes.any { it.second is ConeKotlinErrorType }) return

        var delegation: FirCallableDeclaration<*>? = null
        val implementations = mutableListOf<FirCallableDeclaration<*>>()

        for (intSymbol in symbol.intersections) {
            val fir = intSymbol.fir
            if (fir.delegatedWrapperData?.containingClass?.classId == containingClass.classId) {
                delegation = fir
                break
            }
            if (!(fir as FirCallableMemberDeclaration<*>).isAbstract) {
                implementations.add(fir)
            }
        }

        run {
            var clash: Pair<FirCallableDeclaration<*>, FirCallableDeclaration<*>>? = null
            val compatible = withTypes.any { (m1, type1) ->
                withTypes.all { (m2, type2) ->
                    val result = canOverride(m2, type1, type2)
                    if (!result && clash == null && !canOverride(m1, type2, type1)) {
                        clash = m1 to m2
                    }
                    result
                }
            }
            clash?.takeIf { !compatible }?.let { (m1, m2) ->
                reportTypeMismatch(m1, m2, false)
                return@checkInheritanceClash
            }
        }

        if (delegation != null || implementations.isNotEmpty()) {
            //if there are more than one implementation we report nothing because it will be reported differently
            val implementationMember = delegation ?: implementations.singleOrNull() ?: return
            val methodType = context.returnTypeCalculator.tryCalculateReturnType(implementationMember).coneType
            val (conflict, _) = withTypes.find { (baseMember, baseType) ->
                !canOverride(baseMember, methodType, baseType)
            } ?: return

            reportTypeMismatch(implementationMember, conflict, delegation != null)
        }
    }

    private fun checkValOverrideVar(
        containingClass: FirClass<*>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        symbol: FirVariableSymbol<*>
    ) {
        if (symbol.callableId.classId != containingClass.classId) return
        if (symbol !is FirIntersectionOverridePropertySymbol) return

        val (delegates, others) = symbol.intersections.partition {
            val fir = it.fir as? FirProperty ?: return@partition false
            fir.isVal && fir.delegatedWrapperData?.containingClass?.classId == containingClass.classId
        }

        val delegatedVal = delegates.singleOrNull() ?: return
        val baseVar = others.find {
            it is FirPropertySymbol && it.fir.isVar
        }

        if (baseVar != null) {
            reporter.reportOn(containingClass.source, FirErrors.VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION, delegatedVal.fir, baseVar.fir, context)
        }
    }
}