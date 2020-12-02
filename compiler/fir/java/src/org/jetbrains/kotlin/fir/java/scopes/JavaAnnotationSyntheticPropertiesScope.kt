/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class JavaAnnotationSyntheticPropertiesScope(
    private val session: FirSession,
    owner: FirRegularClassSymbol,
    private val delegateScope: JavaClassMembersEnhancementScope
) : FirTypeScope() {
    private val classId: ClassId = owner.classId
    private val names: Set<Name> = owner.fir.declarations.mapNotNullTo(mutableSetOf()) { (it as? FirSimpleFunction)?.name }
    private val syntheticPropertiesCache = mutableMapOf<FirFunctionSymbol<*>, FirVariableSymbol<*>>()

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        delegateScope.processDeclaredConstructors(processor)
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        if (name in names) return
        delegateScope.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        if (name !in names) return
        delegateScope.processFunctionsByName(name) { functionSymbol ->
            val function = functionSymbol.fir as? FirSimpleFunction ?: return@processFunctionsByName
            val symbol = syntheticPropertiesCache.getOrPut(functionSymbol) {
                val callableId = CallableId(classId, name)
                FirAccessorSymbol(callableId, callableId).also {
                    val accessor = FirSyntheticPropertyAccessor(function, isGetter = true)
                    FirSyntheticProperty(session, name, isVar = false, it, function.status, function.resolvePhase, accessor)
                }
            }
            processor(symbol)
        }
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        delegateScope.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return ProcessorAction.NONE
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return ProcessorAction.NONE
    }

    override fun getCallableNames(): Set<Name> {
        return delegateScope.getCallableNames()
    }

    override fun getClassifierNames(): Set<Name> {
        return delegateScope.getClassifierNames()
    }
}
