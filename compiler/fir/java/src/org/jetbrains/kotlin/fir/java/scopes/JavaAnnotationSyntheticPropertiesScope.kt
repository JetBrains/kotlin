/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.java.symbols.FirJavaOverriddenSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.nullableModuleData
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.scopes.FirDelegatingTypeScope

class JavaAnnotationSyntheticPropertiesScope(
    private val session: FirSession,
    owner: FirRegularClassSymbol,
    private val delegateScope: JavaClassMembersEnhancementScope
) : FirDelegatingTypeScope(delegateScope) {
    private val classId: ClassId = owner.classId
    private val names: Set<Name> = owner.fir.declarations.mapNotNullTo(mutableSetOf()) { (it as? FirSimpleFunction)?.name }
    private val syntheticPropertiesCache = mutableMapOf<FirNamedFunctionSymbol, FirVariableSymbol<*>>()

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        if (name in names) return
        super.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        if (name !in names) return
        delegateScope.processFunctionsByName(name) { functionSymbol ->
            val function = functionSymbol.fir
            val symbol = syntheticPropertiesCache.getOrPut(functionSymbol) {
                val callableId = CallableId(classId, name)
                FirJavaOverriddenSyntheticPropertySymbol(callableId, callableId).also {
                    val accessor = FirSyntheticPropertyAccessor(function, isGetter = true, it)
                    FirSyntheticProperty(
                        session.nullableModuleData ?: function.moduleData,
                        name,
                        isVar = false,
                        it,
                        function.status.copy(modality = Modality.FINAL),
                        function.resolvePhase,
                        accessor
                    )
                }
            }
            processor(symbol)
        }
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

    override fun toString(): String {
        return "Java annotation synthetic properties scope for $classId"
    }
}
