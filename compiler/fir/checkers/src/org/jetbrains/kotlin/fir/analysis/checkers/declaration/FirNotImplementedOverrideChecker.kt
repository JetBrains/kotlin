/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.FirDeclarationPresenter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClass
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenProperties
import org.jetbrains.kotlin.fir.scopes.impl.unwrapDelegateTarget
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.util.OperatorNameConventions.TO_STRING

object FirNotImplementedOverrideChecker : FirClassChecker() {
    // TODO: differentiate members with different annotations, e.g., @Api...(x) @Api...(y)
    // TODO: differentiate parameter names? e.g., foo(x : T) v.s. override foo(y : T)
    private object SignaturePresenter : FirDeclarationPresenter {
        override fun StringBuilder.appendRepresentation(it: CallableId) {
            append(it.callableName)
        }
    }

    override fun check(declaration: FirClass<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        // TODO: kt4763Property: reporting on `object` literal causes invalid error in test...FirDiagnosticHandler
        if (declaration is FirAnonymousObject) return

        val source = declaration.source ?: return
        if (source.kind is FirFakeSourceElementKind) return

        val contributedMembers = collectCallableMembers(declaration, context)
        val potentialFakeOverrides = getBaseDeclarationsForFakeOverrides(declaration, contributedMembers, context)

        // TODO: consider open as overridable, and check conflicts as well
        val abstractFakeOverrides = potentialFakeOverrides.filter {
            it is FirMemberDeclaration &&
                    it.isAbstract &&
                    // TODO: FIR MPP support
                    (it.getContainingClass(context) as? FirRegularClass)?.isExpect == false
        }
        if (abstractFakeOverrides.isEmpty()) return

        // TODO: differentiate type-substituted declarations. Otherwise, they will be reported as MANY_IMPL_MEMBER_NOT_IMPLEMENTED
        val sigToDeclarations = mutableMapOf<String, MutableList<FirCallableDeclaration<*>>>()
        for (abstractFakeOverride in abstractFakeOverrides) {
            val sig = when (abstractFakeOverride) {
                is FirSimpleFunction -> SignaturePresenter.represent(abstractFakeOverride)
                is FirProperty -> SignaturePresenter.represent(abstractFakeOverride)
                else -> continue
            }
            sigToDeclarations.computeIfAbsent(sig) { mutableListOf() }.add(abstractFakeOverride)
        }

        val canHaveAbstractMembers = declaration is FirRegularClass && declaration.canHaveAbstractFakeOverride
        // var alreadyReportedManyNotImplemented = false
        var alreadyReportedAbstractNotImplemented = false
        for (fakeOverrides in sigToDeclarations.values) {
            if (fakeOverrides.size > 1) {
                // TODO: MANY_* as well as some conflict diagnostics
/*
                if (alreadyReportedManyNotImplemented) continue
                val representative = fakeOverrides.first()
                if (fakeOverrides.any { it.isFromInterface(context) }) {
                    reporter.reportOn(source, FirErrors.MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED, declaration, representative, context)
                } else {
                    reporter.reportOn(source, FirErrors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED, declaration, representative, context)
                }
                alreadyReportedManyNotImplemented = true
*/
            } else {
                if (canHaveAbstractMembers || alreadyReportedAbstractNotImplemented) continue
                val notImplemented = fakeOverrides.single()
                if ((notImplemented as? FirMemberDeclaration)?.isAbstract != true) continue

                val notImplementedFunction = notImplemented as? FirSimpleFunction
                // TODO: suspend function overridden by a Java class in the middle is not properly regarded as an override.
                if (notImplementedFunction?.isSuspend == true) continue

                val original = notImplementedFunction?.unwrapFakeOverrides()
                // TODO: Generalization? e.g., allowing a fake override for a built-in name?
                if (original?.symbol?.callableId?.toString() == "kotlin/collections/MutableList.removeAt") continue

                if (notImplemented.isFromInterface(context)) {
                    reporter.reportOn(source, FirErrors.ABSTRACT_MEMBER_NOT_IMPLEMENTED, declaration, notImplemented, context)
                } else {
                    reporter.reportOn(source, FirErrors.ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED, declaration, notImplemented, context)
                }
                alreadyReportedAbstractNotImplemented = true
            }
        }
    }

    private fun FirCallableDeclaration<*>.isFromInterface(context: CheckerContext): Boolean =
        (getContainingClass(context) as? FirRegularClass)?.isInterface == true

    private fun collectCallableMembers(firClass: FirClass<*>, context: CheckerContext): Collection<FirCallableDeclaration<*>> =
        firClass.declarations.filterIsInstance<FirCallableDeclaration<*>>() +
                dataClassMembers(firClass, context) +
                delegatedMembers(firClass, context)

    // NB: intentionally not check return types
    private val FirSimpleFunction.matchesEqualsSignature: Boolean
        get() = valueParameters.size == 1 && valueParameters[0].returnTypeRef.coneType.isNullableAny

    private val FirSimpleFunction.matchesHashCodeSignature: Boolean
        get() = valueParameters.isEmpty()

    private val FirSimpleFunction.matchesToStringSignature: Boolean
        get() = valueParameters.isEmpty()

    private val FirSimpleFunction.matchesDataClassSyntheticMemberSignatures: Boolean
        get() = (this.name == EQUALS && matchesEqualsSignature) ||
                (this.name == HASHCODE_NAME && matchesHashCodeSignature) ||
                (this.name == TO_STRING && matchesToStringSignature)

    private val SYNTHETIC_NAMES = listOf(EQUALS, HASHCODE_NAME, TO_STRING)

    // See [DataClassMembersGenerator#generate]
    // But, this one doesn't create IR counterparts. We just need what synthetic members would be generated for data/inline classes.
    private fun dataClassMembers(firClass: FirClass<*>, context: CheckerContext): Collection<FirCallableDeclaration<*>> {
        if (firClass !is FirRegularClass || (!firClass.isData && !firClass.isInline)) return emptyList()

        val contributedInThisType = firClass.declarations.mapNotNull {
            if (it is FirSimpleFunction && it.matchesDataClassSyntheticMemberSignatures) {
                it.name
            } else
                null
        }

        val contributedInSupertypes = mutableMapOf<Name, FirCallableDeclaration<*>>()
        val classScope = firClass.unsubstitutedScope(context)
        for (name in SYNTHETIC_NAMES) {
            classScope.processFunctionsByName(name) {
                val declaration = it.fir
                if (declaration.matchesDataClassSyntheticMemberSignatures && declaration.modality != Modality.FINAL) {
                    contributedInSupertypes.putIfAbsent(declaration.name, declaration)
                }
            }
        }

        val result = mutableListOf<FirCallableDeclaration<*>>()
        for (name in SYNTHETIC_NAMES) {
            if (name in contributedInThisType) continue
            contributedInSupertypes[name]?.let { result.add(it) }
        }
        return result
    }

    // See [DelegatedMemberGenerator#generate]
    // But, this one doesn't create IR counterparts. We just need what members could be delegated to avoid false fake overrides.
    private fun delegatedMembers(firClass: FirClass<*>, context: CheckerContext): Collection<FirCallableDeclaration<*>> {
        val delegations = firClass.declarations.filterIsInstance<FirField>().filter { it.isSynthetic }
        val delegation = delegations.singleOrNull() ?: return emptyList()

        val result = mutableListOf<FirCallableDeclaration<*>>()
        val classLookupTag = firClass.symbol.toLookupTag()
        val classScope = firClass.unsubstitutedScope(context)

        classScope.processAllFunctions { functionSymbol ->
            val unwrapped =
                functionSymbol.unwrapDelegateTarget(classLookupTag, classScope::getDirectOverriddenFunctions, delegation, firClass)
                    ?: return@processAllFunctions

            if (unwrapped.isJavaDefault) {
                return@processAllFunctions
            }

            result.add(functionSymbol.fir)
        }

        classScope.processAllProperties { propertySymbol ->
            if (propertySymbol !is FirPropertySymbol) return@processAllProperties

            propertySymbol.unwrapDelegateTarget(classLookupTag, classScope::getDirectOverriddenProperties, delegation, firClass)
                ?: return@processAllProperties

            result.add(propertySymbol.fir)
        }

        return result
    }

    // See [FakeOverrideGenerator#getFakeOverrides]
    // But, this one doesn't create fake overrides. We just need what _base_ declarations will be referred by fake overrides.
    private fun getBaseDeclarationsForFakeOverrides(
        firClass: FirClass<*>,
        contributedDeclarations: Collection<FirDeclaration>,
        context: CheckerContext
    ): Collection<FirCallableDeclaration<*>> {
        val result = mutableListOf<FirCallableDeclaration<*>>()
        val classScope = firClass.unsubstitutedScope(context)

        fun checkFunctionSymbolAndAddToResult(originalSymbol: FirCallableSymbol<*>) {
            if (shouldCreateFakeOverridden<FirSimpleFunction, FirNamedFunctionSymbol>(firClass, originalSymbol, contributedDeclarations)) {
                result.add(originalSymbol.fir)
            }
        }

        fun checkPropertySymbolAndAddToResult(originalSymbol: FirCallableSymbol<*>) {
            if (shouldCreateFakeOverridden<FirProperty, FirPropertySymbol>(firClass, originalSymbol, contributedDeclarations)) {
                result.add(originalSymbol.fir)
            }
        }

        val superTypesCallableNames = classScope.getCallableNames()
        for (name in superTypesCallableNames) {
            classScope.processFunctionsByName(name) { functionSymbol ->
                // TODO: MANY_* as well as some conflict diagnostics
                //if (functionSymbol is FirIntersectionOverrideFunctionSymbol)
                //    functionSymbol.intersections.forEach(::checkFunctionSymbolAndAddToResult)
                //else
                checkFunctionSymbolAndAddToResult(functionSymbol)
            }

            classScope.processPropertiesByName(name) { propertySymbol ->
                // TODO: MANY_* as well as some conflict diagnostics
                //if (propertySymbol is FirIntersectionOverridePropertySymbol)
                //    propertySymbol.intersections.forEach(::checkPropertySymbolAndAddToResult)
                //else
                checkPropertySymbolAndAddToResult(propertySymbol)
            }
        }

        return result
    }

    // See [FakeOverrideGenerator#createFakeOverriddenIfNeeded]
    private inline fun <reified D : FirCallableMemberDeclaration<D>, reified S : FirCallableSymbol<D>> shouldCreateFakeOverridden(
        firClass: FirClass<*>,
        originalSymbol: FirCallableSymbol<*>,
        contributedDeclarations: Collection<FirDeclaration>,
    ): Boolean {
        if (originalSymbol !is S || originalSymbol.fir in contributedDeclarations) return false
        val classLookupTag = firClass.symbol.toLookupTag()
        val originalDeclaration = originalSymbol.fir
        if (originalSymbol.dispatchReceiverClassOrNull() == classLookupTag && !originalDeclaration.origin.fromSupertypes) return false
        if (originalDeclaration.visibility == Visibilities.Private) return false
        return when {
            originalSymbol.fir.origin.fromSupertypes && originalSymbol.dispatchReceiverClassOrNull() == classLookupTag -> {
                // Substitution case
                false
            }
            originalDeclaration.allowsToHaveFakeOverrideIn(firClass) -> {
                // Trivial fake override case
                true
            }
            else -> {
                false
            }
        }
    }

    // TODO: Need to refactor the counterpart in FakeOverrideGenerator?
    private fun FirCallableMemberDeclaration<*>.allowsToHaveFakeOverrideIn(firClass: FirClass<*>): Boolean {
        if (!allowsToHaveFakeOverride) return false
        // if (this.visibility != JavaDescriptorVisibilities.PACKAGE_VISIBILITY) return true
        return this.symbol.callableId.packageName == firClass.symbol.classId.packageFqName
    }
}
