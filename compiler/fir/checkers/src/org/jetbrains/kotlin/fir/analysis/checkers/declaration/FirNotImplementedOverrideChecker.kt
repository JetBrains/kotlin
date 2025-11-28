/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.DelegatedWrapperData
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_MEMBER_INCORRECTLY_DELEGATED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_MEMBER_NOT_IMPLEMENTED_BY_ENUM_ENTRY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.OVERRIDING_FINAL_MEMBER_BY_DELEGATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.VAR_IMPLEMENTED_BY_INHERITED_VAL
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.delegatedWrapperData
import org.jetbrains.kotlin.fir.isVisibleInClass
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.MemberWithBaseScope
import org.jetbrains.kotlin.fir.scopes.ScopeFunctionRequiresPrewarm
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenMembersWithBaseScope
import org.jetbrains.kotlin.fir.scopes.impl.filterOutOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.impl.filterOutOverriddenProperties
import org.jetbrains.kotlin.fir.scopes.impl.multipleDelegatesWithTheSameSignature
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.util.ImplementationStatus
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object FirNotImplementedOverrideChecker : FirClassChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (declaration.isExpect && !context.languageVersionSettings.getFlag(AnalysisFlags.metadataCompilation)) return
        val source = declaration.source ?: return
        val sourceKind = source.kind
        if (sourceKind is KtFakeSourceElementKind && sourceKind != KtFakeSourceElementKind.EnumInitializer) return
        val modality = declaration.modality()
        val classKind = declaration.classKind
        if (classKind == ClassKind.ANNOTATION_CLASS || classKind == ClassKind.ENUM_CLASS) return
        val canHaveAbstractDeclarations = modality == Modality.ABSTRACT || modality == Modality.SEALED ||
                classKind == ClassKind.INTERFACE && modality == Modality.OPEN
        val classSymbol = declaration.symbol

        val classScope = declaration.unsubstitutedScope()

        val notImplementedSymbols = mutableListOf<FirCallableSymbol<*>>()
        val notImplementedIntersectionSymbols = mutableListOf<FirCallableSymbol<*>>()
        val manyImplementationsDelegationSymbols = mutableListOf<FirCallableSymbol<*>>()
        val delegationOverrideOfFinal = mutableListOf<Pair<FirCallableSymbol<*>, FirCallableSymbol<*>>>()
        val delegationOverrideOfOpen = mutableListOf<Pair<FirCallableSymbol<*>, FirCallableSymbol<*>>>()
        val invisibleSymbols = mutableListOf<FirCallableSymbol<*>>()
        val varsImplementedByInheritedVal = mutableListOf<FirIntersectionCallableSymbol>()
        val incorrectlyDelegatedSymbols = mutableListOf<FirCallableSymbol<*>>()
        val session = context.session

        fun DelegatedWrapperData<FirCallableDeclaration>.isIncorrectlyDelegated(): Boolean {
            if (classKind != ClassKind.OBJECT) return false
            val delegateFieldType = delegateFieldSymbol.resolvedInitializer?.resolvedType?.fullyExpandedType()
            return (delegateFieldType as? ConeClassLikeType)?.lookupTag?.toSymbol() == classSymbol
        }

        @OptIn(ScopeFunctionRequiresPrewarm::class) // The symbol is coming from a call to process*ByName
        fun collectSymbol(symbol: FirCallableSymbol<*>) {
            val delegatedWrapperData = symbol.delegatedWrapperData
            if (delegatedWrapperData != null && symbol !is FirIntersectionCallableSymbol) {
                val directOverriddenMembersWithBaseScope = classScope
                    .getDirectOverriddenMembersWithBaseScope(symbol)
                    .filter { it.member != symbol }

                @Suppress("UNCHECKED_CAST")
                val filteredOverriddenMembers = when (symbol) {
                    is FirNamedFunctionSymbol -> filterOutOverriddenFunctions(directOverriddenMembersWithBaseScope as List<MemberWithBaseScope<FirNamedFunctionSymbol>>)
                    is FirPropertySymbol -> filterOutOverriddenProperties(directOverriddenMembersWithBaseScope as List<MemberWithBaseScope<FirPropertySymbol>>)
                    else -> directOverriddenMembersWithBaseScope
                }.map { it.member }

                val delegatedTo = delegatedWrapperData.wrapped.unwrapFakeOverrides().symbol

                if (symbol.multipleDelegatesWithTheSameSignature == true) {
                    manyImplementationsDelegationSymbols.add(symbol)
                }

                val firstFinal = filteredOverriddenMembers.firstOrNull { it.isFinal }
                val firstOpen = filteredOverriddenMembers.firstOrNull { it.isOpen && delegatedTo != it.unwrapFakeOverrides() }

                when {
                    firstFinal != null ->
                        delegationOverrideOfFinal.add(symbol to firstFinal)

                    firstOpen != null ->
                        delegationOverrideOfOpen.add(symbol to firstOpen)
                }

                if (delegatedWrapperData.isIncorrectlyDelegated()) {
                    incorrectlyDelegatedSymbols.add(symbol)
                }
                return
            }
            when (symbol.getImplementationStatus(classSymbol)) {
                ImplementationStatus.AMBIGUOUSLY_INHERITED -> notImplementedIntersectionSymbols.add(symbol)
                ImplementationStatus.NOT_IMPLEMENTED -> when {
                    symbol.isVisibleInClass(classSymbol) -> notImplementedSymbols.add(symbol)
                    else -> invisibleSymbols.add(symbol)
                }
                ImplementationStatus.VAR_IMPLEMENTED_BY_VAL -> varsImplementedByInheritedVal.add(symbol as FirIntersectionCallableSymbol)
                else -> {
                    // nothing to do
                }
            }
        }

        for (name in classScope.getCallableNames()) {
            classScope.processFunctionsByName(name, ::collectSymbol)
            classScope.processPropertiesByName(name, ::collectSymbol)
        }

        varsImplementedByInheritedVal.forEach { symbol ->
            val implementationVal = symbol.intersections.first { it is FirPropertySymbol && it.isVal && !it.isAbstract }
            val abstractVar = symbol.intersections.first { it is FirPropertySymbol && it.isVar && it.isAbstract }
            reporter.reportOn(
                source,
                VAR_IMPLEMENTED_BY_INHERITED_VAL,
                classSymbol,
                abstractVar,
                implementationVal,
            )
        }
        if (!canHaveAbstractDeclarations) {
            val (fromInterfaceOrEnum, notFromInterfaceOrEnum) = notImplementedSymbols.partition {
                it.unwrapFakeOverrides().isFromInterfaceOrEnum()
            }
            val containingDeclaration = context.containingDeclarations.lastOrNull()
            val (fromInitializerOfEnumEntry, notFromInitializerOfEnumEntry) = fromInterfaceOrEnum.partition {
                declaration.isInitializerOfEnumEntry(containingDeclaration)
            }

            if (containingDeclaration is FirEnumEntrySymbol && fromInitializerOfEnumEntry.isNotEmpty()) {
                reporter.reportOn(
                    source,
                    ABSTRACT_MEMBER_NOT_IMPLEMENTED_BY_ENUM_ENTRY,
                    containingDeclaration,
                    fromInitializerOfEnumEntry
                )
            }

            if (notFromInitializerOfEnumEntry.isNotEmpty()) {
                reporter.reportOn(
                    source,
                    ABSTRACT_MEMBER_NOT_IMPLEMENTED,
                    classSymbol,
                    notFromInitializerOfEnumEntry.map { it.unwrapFakeOverrides() })
            }

            if (notFromInterfaceOrEnum.isNotEmpty()) {
                reporter.reportOn(
                    source,
                    ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED,
                    classSymbol,
                    notFromInterfaceOrEnum.map { it.unwrapFakeOverrides() })
            }

            if (incorrectlyDelegatedSymbols.isNotEmpty()) {
                reporter.reportOn(
                    source,
                    ABSTRACT_MEMBER_INCORRECTLY_DELEGATED,
                    classSymbol,
                    incorrectlyDelegatedSymbols.map { it.unwrapFakeOverrides() }
                )
            }
        }
        if (!canHaveAbstractDeclarations && invisibleSymbols.isNotEmpty()) {
            reporter.reportOn(source, INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR, classSymbol, invisibleSymbols)
        }

        manyImplementationsDelegationSymbols.forEach {
            reporter.reportOn(source, MANY_IMPL_MEMBER_NOT_IMPLEMENTED, classSymbol, it)
        }

        delegationOverrideOfFinal.forEach { (delegated, final) ->
            reporter.reportOn(
                source,
                OVERRIDING_FINAL_MEMBER_BY_DELEGATION,
                delegated,
                final
            )
        }

        delegationOverrideOfOpen.forEach { (delegated, open) ->
            reporter.reportOn(
                source,
                DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE,
                delegated,
                open
            )
        }

        if (manyImplementationsDelegationSymbols.isEmpty()) {
            notImplementedIntersectionSymbols.forEach { notImplementedIntersectionSymbol ->
                val (abstractIntersections, implIntersections) =
                    (notImplementedIntersectionSymbol as FirIntersectionCallableSymbol).intersections.partition {
                        it.modality == Modality.ABSTRACT
                    }
                if (implIntersections.any {
                        it.containingClassLookupTag()?.toRegularClassSymbol()?.classKind == ClassKind.CLASS
                    }
                ) {
                    reporter.reportOn(source, MANY_IMPL_MEMBER_NOT_IMPLEMENTED, classSymbol, notImplementedIntersectionSymbol)
                } else {
                    if (canHaveAbstractDeclarations && abstractIntersections.any {
                            it.containingClassLookupTag()?.toRegularClassSymbol()?.classKind == ClassKind.CLASS
                        }
                    ) {
                        return
                    }
                    reporter.reportOn(source, MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED, classSymbol, notImplementedIntersectionSymbol)
                }
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    private fun FirClass.isInitializerOfEnumEntry(containingDeclaration: FirBasedSymbol<*>?): Boolean {
        contract {
            returns(true) implies (containingDeclaration is FirEnumEntrySymbol)
        }
        return containingDeclaration is FirEnumEntrySymbol && containingDeclaration.initializerObjectSymbol == this.symbol
    }

    private fun FirCallableSymbol<*>.isFromInterfaceOrEnum(): Boolean =
        (getContainingClassSymbol() as? FirRegularClassSymbol)?.let { it.isInterface || it.isEnumClass } == true
}
