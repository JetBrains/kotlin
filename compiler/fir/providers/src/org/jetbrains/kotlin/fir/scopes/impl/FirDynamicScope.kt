/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

class FirDynamicScope(
    private val session: FirSession,
) : FirTypeScope() {
    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction = ProcessorAction.NEXT

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction = ProcessorAction.NEXT

    override fun getCallableNames(): Set<Name> = emptySet()

    override fun getClassifierNames(): Set<Name> = emptySet()

    override fun processFunctionsByName(
        name: Name,
        processor: (FirNamedFunctionSymbol) -> Unit
    ) {
        val function = pseudoFunctions.getOrPut(name) {
            buildPseudoFunctionByName(name)
        }

        processor(function.symbol)
    }

    override fun processPropertiesByName(
        name: Name,
        processor: (FirVariableSymbol<*>) -> Unit
    ) {
        val property = pseudoProperties.getOrPut(name) {
            buildPseudoPropertyByName(name)
        }

        processor(property.symbol)
    }

    private val dynamicTypeRef = buildResolvedTypeRef {
        type = ConeDynamicType(
            session.builtinTypes.nothingType.type,
            session.builtinTypes.nullableAnyType.type,
        )
    }

    private val anyArrayTypeRef = buildResolvedTypeRef {
        type = ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagImpl(StandardClassIds.Array),
            arrayOf(session.builtinTypes.nullableAnyType.coneType),
            isNullable = false
        )
    }

    private val pseudoFunctions = mutableMapOf<Name, FirSimpleFunction>()

    private fun buildPseudoFunctionByName(name: Name) = buildSimpleFunction {
        status = FirResolvedDeclarationStatusImpl(
            Visibilities.Local,
            Modality.FINAL,
            EffectiveVisibility.Local,
        ).apply {
            isInfix = true
            isOperator = true
        }

        this.name = name
//        this.name = Name.identifier("KOK" + name.toString())
        this.symbol = FirNamedFunctionSymbol(CallableId(this.name))

        moduleData = session.moduleData
        origin = FirDeclarationOrigin.DynamicScope
        returnTypeRef = dynamicTypeRef

        val parameter = buildValueParameter {
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.DynamicScope
            returnTypeRef = anyArrayTypeRef
            this.name = Name.identifier("args")
            this.symbol = FirValueParameterSymbol(this.name)
            isCrossinline = false
            isNoinline = false
            isVararg = true
        }

        valueParameters.add(parameter)
    }

    private val pseudoProperties = mutableMapOf<Name, FirProperty>()

    private fun buildPseudoPropertyByName(name: Name) = buildProperty {
        this.name = name
        this.symbol = FirPropertySymbol(CallableId(this.name))

        status = FirResolvedDeclarationStatusImpl(
            Visibilities.Local,
            Modality.FINAL,
            EffectiveVisibility.Local,
        )

        moduleData = session.moduleData
        origin = FirDeclarationOrigin.DynamicScope
        returnTypeRef = dynamicTypeRef
        isVar = true
        isLocal = false
    }
}