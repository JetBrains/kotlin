/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.impl.deduplicating
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isVisibleInClass
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.delegatedWrapperData
import org.jetbrains.kotlin.fir.isSubstitutionOverride
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.unwrapSubstitutionOverrides
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.utils.addIfNotNull

sealed class FirImplementationMismatchChecker(mppKind: MppCheckerKind) : FirClassChecker(mppKind) {
    object Regular : FirImplementationMismatchChecker(MppCheckerKind.Platform) {
        override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
            if (declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    object ForExpectClass : FirImplementationMismatchChecker(MppCheckerKind.Common) {
        override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
            if (!declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = declaration.source ?: return
        val sourceKind = source.kind
        if (sourceKind is KtFakeSourceElementKind && sourceKind != KtFakeSourceElementKind.EnumInitializer) return
        if (declaration is FirRegularClass && declaration.isExpect) return
        val classKind = declaration.classKind
        if (classKind == ClassKind.ANNOTATION_CLASS || classKind == ClassKind.ENUM_CLASS) return

        val typeCheckerState = context.session.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )
        val classScope = declaration.unsubstitutedScope(context)
        val dedupReporter = reporter.deduplicating()

        for (name in classScope.getCallableNames()) {
            classScope.processFunctionsByName(name) {
                checkInheritanceClash(declaration, context, dedupReporter, typeCheckerState, it, classScope)
            }
            classScope.processPropertiesByName(name) {
                checkInheritanceClash(declaration, context, dedupReporter, typeCheckerState, it, classScope)
                checkValOverridesVar(declaration, context, dedupReporter, it, classScope)
            }
            checkConflictingMembers(declaration, context, dedupReporter, classScope, name)
        }
    }

    private fun checkInheritanceClash(
        containingClass: FirClass,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        typeCheckerState: TypeCheckerState,
        symbol: FirCallableSymbol<*>,
        classScope: FirTypeScope
    ) {
        fun reportTypeMismatch(member1: FirCallableSymbol<*>, member2: FirCallableSymbol<*>, isDelegation: Boolean) {
            val error = when {
                member1 is FirPropertySymbol && member2 is FirPropertySymbol -> {
                    if (member1.isVar || member2.isVar) {
                        FirErrors.VAR_TYPE_MISMATCH_ON_INHERITANCE
                    } else {
                        if (isDelegation) FirErrors.PROPERTY_TYPE_MISMATCH_BY_DELEGATION
                        else FirErrors.PROPERTY_TYPE_MISMATCH_ON_INHERITANCE
                    }
                }
                else -> {
                    if (isDelegation) FirErrors.RETURN_TYPE_MISMATCH_BY_DELEGATION
                    else FirErrors.RETURN_TYPE_MISMATCH_ON_INHERITANCE
                }
            }
            reporter.reportOn(containingClass.source, error, member1, member2, context)
        }

        fun canOverride(
            inheritedMember: FirCallableSymbol<*>,
            inheritedType: ConeKotlinType,
            baseMember: FirCallableSymbol<*>,
            baseType: ConeKotlinType
        ): Boolean {
            val inheritedTypeSubstituted = inheritedType.substituteTypeParameters(inheritedMember, baseMember, context)
            return if (baseMember is FirPropertySymbol && baseMember.isVar)
                AbstractTypeChecker.equalTypes(typeCheckerState, inheritedTypeSubstituted, baseType)
            else
                AbstractTypeChecker.isSubtypeOf(typeCheckerState, inheritedTypeSubstituted, baseType)
        }

        /**
         * An intersection override is trivial if one of the overridden symbols subsumes all others.
         *
         * @see org.jetbrains.kotlin.fir.scopes.impl.FirTypeIntersectionScopeContext.convertGroupedCallablesToIntersectionResults
         */
        fun FirCallableSymbol<*>.isTrivialIntersectionOverride(): Boolean {
            return callableId.classId != containingClass.classId || MemberWithBaseScope(this, classScope).isTrivialIntersection()
        }

        val intersectionSymbols = when {
            //substitution override means simple materialization of single method, so nothing to check
            symbol.isSubstitutionOverride -> return
            symbol.delegatedWrapperData != null -> {
                val allOverridden = classScope.getDirectOverriddenMembers(symbol)
                //if there is intersection override - take its intersections - they will contain all substitutions
                //otherwise we get base members with unsubstituted params too
                val cleared = allOverridden.find { it is FirIntersectionCallableSymbol }?.let {
                    (it as FirIntersectionCallableSymbol).intersections
                } ?: allOverridden
                //current symbol needs to be added, because basically it is the implementation
                cleared + symbol
            }
            symbol is FirIntersectionCallableSymbol && !symbol.isTrivialIntersectionOverride() ->
                // We intentionally don't use getNonSubsumedOverriddenSymbols here, otherwise we'll get lots of new errors (compared to K1)
                // in cases where a Java superclass inherits multiple members with conflicting nullability annotations.
                symbol.intersections
            else -> return
        }

        val withTypes = intersectionSymbols.map {
            it to context.returnTypeCalculator.tryCalculateReturnType(it).coneType
        }

        if (withTypes.any { it.second is ConeErrorType }) return

        var delegation: FirCallableSymbol<*>? = null
        val implementations = mutableListOf<FirCallableSymbol<*>>()

        for (intSymbol in intersectionSymbols) {
            if (intSymbol.delegatedWrapperData?.containingClass?.classId == containingClass.classId) {
                delegation = intSymbol
                break
            }
            if (!intSymbol.isAbstract) {
                implementations.add(intSymbol)
            }
        }

        var someClash: Pair<FirCallableSymbol<*>, FirCallableSymbol<*>>? = null
        val compatible = withTypes.any { (m1, type1) ->
            withTypes.all { (m2, type2) ->
                val result = canOverride(m1, type1, m2, type2)
                if (!result && someClash == null && !canOverride(m2, type2, m1, type1)) {
                    someClash = m1 to m2
                }
                result
            }
        }
        someClash?.takeIf { !compatible }?.let { (m1, m2) ->
            reportTypeMismatch(m1, m2, false)
            return@checkInheritanceClash
        }

        if (delegation != null || implementations.isNotEmpty()) {
            //if there are more than one implementation we report nothing because it will be reported differently
            val implementationMember = delegation ?: implementations.singleOrNull() ?: return
            val implementationType = context.returnTypeCalculator.tryCalculateReturnType(implementationMember).coneType
            val (conflict, _) = withTypes.find { (baseMember, baseType) ->
                !canOverride(implementationMember, implementationType, baseMember, baseType)
            } ?: return

            reportTypeMismatch(implementationMember, conflict, delegation != null)
        }
    }

    private fun checkValOverridesVar(
        containingClass: FirClass,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        symbol: FirVariableSymbol<*>,
        classScope: FirTypeScope
    ) {
        if (symbol !is FirPropertySymbol || symbol.isVar) return
        if (symbol.delegatedWrapperData == null) return

        val overriddenVar =
            classScope.getDirectOverriddenProperties(symbol, true)
                .find { it.isVar }
                ?: return

        reporter.reportOn(containingClass.source, FirErrors.VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION, symbol, overriddenVar, context)
    }

    private fun FirTypeScope.collectFunctionsNamed(
        name: Name,
        containingClass: FirClass,
        context: CheckerContext,
    ): List<FirNamedFunctionSymbol> {
        val allFunctions = mutableListOf<FirNamedFunctionSymbol>()

        processFunctionsByName(name) { sym ->
            when (sym) {
                is FirIntersectionOverrideFunctionSymbol -> sym
                    .getNonSubsumedOverriddenSymbols(context.session, context.scopeSession)
                    .mapNotNullTo(allFunctions) { it as? FirNamedFunctionSymbol }
                else -> allFunctions.add(sym)
            }
        }

        return allFunctions.filter {
            it.isVisibleInClass(containingClass.symbol)
        }
    }

    private fun checkConflictingMembers(
        containingClass: FirClass,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        scope: FirTypeScope,
        name: Name
    ) {
        val allFunctions = scope.collectFunctionsNamed(name, containingClass, context)

        val sameArgumentGroups = allFunctions.groupBy { function ->
            buildList {
                addIfNotNull(function.resolvedReceiverTypeRef?.type)
                function.valueParameterSymbols.mapTo(this) { it.resolvedReturnTypeRef.coneType }
            }
        }.values

        val clashes = sameArgumentGroups.mapNotNull { fs ->
            fs.zipWithNext().find { (m1, m2) ->
                m1.isSuspend != m2.isSuspend || m1.typeParameterSymbols.size != m2.typeParameterSymbols.size
            }
        }

        for (clash in clashes) {
            val (first, second) = clash

            val firstClassLookupTag = first.containingClassLookupTag()
            val secondClassLookupTag = second.containingClassLookupTag()

            if (firstClassLookupTag == secondClassLookupTag) {
                // Don't report if both declarations came from the same class because either CONFLICTING_OVERLOADS was reported in the
                // original class or it's ok to keep compatibility with K1. See KT-55860.
                continue
            }

            val thisClassLookupTag = containingClass.symbol.toLookupTag()

            // If one of the declarations is from this class, report CONFLICTING_OVERLOADS, otherwise CONFLICTING_INHERITED_MEMBERS
            if (firstClassLookupTag == thisClassLookupTag && first.source?.kind is KtRealSourceElementKind) {
                reporter.reportOn(first.source, FirErrors.CONFLICTING_OVERLOADS, clash.toList(), context)
            } else if (secondClassLookupTag == thisClassLookupTag && second.source?.kind is KtRealSourceElementKind) {
                reporter.reportOn(second.source, FirErrors.CONFLICTING_OVERLOADS, clash.toList(), context)
            } else {
                reporter.reportOn(
                    containingClass.source, FirErrors.CONFLICTING_INHERITED_MEMBERS, containingClass.symbol, clash.toList(), context,
                )
            }
        }
    }

    private fun ConeKotlinType.substituteTypeParameters(
        fromDeclaration: FirCallableSymbol<*>,
        toDeclaration: FirCallableSymbol<*>,
        context: CheckerContext
    ): ConeKotlinType {
        val fromParams = fromDeclaration.typeParameterSymbols
        val toParams = toDeclaration.typeParameterSymbols

        val substitutionMap = fromParams.zip(toParams) { from, to ->
            from to to.toConeType()
        }.toMap()

        return substitutorByMap(substitutionMap, context.session).substituteOrSelf(this)
    }
}
