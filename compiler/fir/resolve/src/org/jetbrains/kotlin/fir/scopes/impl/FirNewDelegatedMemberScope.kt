/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolved
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.Name

class FirNewDelegatedMemberScope(
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    private val containingClass: FirClass<*>,
    private val declaredMemberScope: FirScope,
    private val delegateFields: List<FirField>,
) : FirScope() {
    private val dispatchReceiverType = containingClass.defaultType()
    private val overrideChecker = FirStandardOverrideChecker(session)

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        declaredMemberScope.processFunctionsByName(name, processor)
        val result = mutableListOf<FirNamedFunctionSymbol>()

        for (delegateField in delegateFields) {
            processFunctionsFromSpecificField(delegateField, name, result)
        }

        result.forEach(processor)
    }

    private fun buildScope(delegateField: FirField): FirTypeScope? {
        delegateField.ensureResolved(FirResolvePhase.TYPES, session)
        return delegateField.returnTypeRef.coneType.scope(session, scopeSession, FakeOverrideTypeCalculator.DoNothing)
    }

    private fun processFunctionsFromSpecificField(
        delegateField: FirField,
        name: Name,
        result: MutableList<FirNamedFunctionSymbol>
    ) {
        val scope = buildScope(delegateField) ?: return

        scope.processFunctionsByName(name) processor@{ functionSymbol ->
            val original = functionSymbol.fir
            // KT-6014: If the original is abstract, we still need a delegation
            // For example,
            //   interface IBase { override fun toString(): String }
            //   object BaseImpl : IBase { override fun toString(): String = ... }
            //   class Test : IBase by BaseImpl
            if (original.isPublicInAny() && original.modality != Modality.ABSTRACT) {
                return@processor
            }

            if (original.modality == Modality.FINAL || original.visibility == Visibilities.Private) {
                return@processor
            }

            if (declaredMemberScope.getFunctions(name).any { overrideChecker.isOverriddenFunction(it.fir, original) }) {
                return@processor
            }

            result.firstOrNull {
                overrideChecker.isOverriddenFunction(it.fir, original)
            }?.let {
                it.fir.multipleDelegatesWithTheSameSignature = true
                return@processor
            }

            val delegatedSymbol =
                FirFakeOverrideGenerator.createCopyForFirFunction(
                    FirNamedFunctionSymbol(
                        functionSymbol.callableId,
                    ),
                    original,
                    session,
                    FirDeclarationOrigin.Delegated,
                    newDispatchReceiverType = dispatchReceiverType,
                    newModality = Modality.OPEN,
                ).apply {
                    delegatedWrapperData = DelegatedWrapperData(functionSymbol.fir, containingClass.symbol.toLookupTag(), delegateField)
                }.symbol

            result += delegatedSymbol
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        declaredMemberScope.processPropertiesByName(name, processor)

        val result = mutableListOf<FirPropertySymbol>()
        for (delegateField in delegateFields) {
            processPropertiesFromSpecificField(delegateField, name, result)
        }

        result.forEach(processor)
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        declaredMemberScope.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        declaredMemberScope.processDeclaredConstructors(processor)
    }

    private fun processPropertiesFromSpecificField(
        delegateField: FirField,
        name: Name,
        result: MutableList<FirPropertySymbol>
    ) {
        val scope = buildScope(delegateField) ?: return

        scope.processPropertiesByName(name) processor@{ propertySymbol ->
            if (propertySymbol !is FirPropertySymbol) {
                return@processor
            }

            val original = propertySymbol.fir

            if (original.modality == Modality.FINAL || original.visibility == Visibilities.Private) {
                return@processor
            }

            if (declaredMemberScope.getProperties(name)
                    .any { it is FirPropertySymbol && overrideChecker.isOverriddenProperty(it.fir, original) }
            ) {
                return@processor
            }


            result.firstOrNull {
                overrideChecker.isOverriddenProperty(it.fir, original)
            }?.let {
                it.fir.multipleDelegatesWithTheSameSignature = true
                return@processor
            }

            val delegatedSymbol =
                FirFakeOverrideGenerator.createCopyForFirProperty(
                    FirPropertySymbol(
                        propertySymbol.callableId
                    ),
                    original,
                    session,
                    FirDeclarationOrigin.Delegated,
                    newModality = Modality.OPEN,
                    newDispatchReceiverType = dispatchReceiverType,
                ).apply {
                    delegatedWrapperData = DelegatedWrapperData(propertySymbol.fir, containingClass.symbol.toLookupTag(), delegateField)
                }.symbol
            result += delegatedSymbol
        }
    }
}

private object MultipleDelegatesWithTheSameSignatureKey : FirDeclarationDataKey()

var FirCallableDeclaration<*>.multipleDelegatesWithTheSameSignature: Boolean? by FirDeclarationDataRegistry.data(MultipleDelegatesWithTheSameSignatureKey)
