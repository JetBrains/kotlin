/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.java.enhancement.FirSignatureEnhancement
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.FirFakeOverrideGenerator
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmDescriptorReplacingKotlinToJava
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class JavaClassMembersEnhancementScope(
    session: FirSession,
    private val owner: FirRegularClassSymbol,
    private val useSiteMemberScope: JavaClassUseSiteMemberScope,
) : FirTypeScope() {
    private val overriddenFunctions = mutableMapOf<FirFunctionSymbol<*>, Collection<FirFunctionSymbol<*>>>()
    private val overriddenProperties = mutableMapOf<FirPropertySymbol, Collection<FirPropertySymbol>>()

    private val overrideBindCache = mutableMapOf<Name, Map<FirCallableSymbol<*>?, List<FirCallableSymbol<*>>>>()
    private val signatureEnhancement = FirSignatureEnhancement(owner.fir, session) {
        overriddenMembers(name)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        useSiteMemberScope.processPropertiesByName(name) process@{ original ->
            val enhancedPropertySymbol = signatureEnhancement.enhancedProperty(original, name)

            if (enhancedPropertySymbol is FirPropertySymbol) {
                val enhancedProperty = enhancedPropertySymbol.fir
                overriddenProperties[enhancedPropertySymbol] =
                    enhancedProperty
                        .overriddenMembers(enhancedProperty.name)
                        .mapNotNull { it.symbol as? FirPropertySymbol }
            }

            processor(enhancedPropertySymbol)
        }

        return super.processPropertiesByName(name, processor)
    }

    private fun FirSimpleFunction.changeSignatureIfErasedValueParameter(): FirSimpleFunction {
        val typeParameters = owner.fir.typeParameters
        if (typeParameters.isEmpty() || name !in SpecialGenericSignatures.ERASED_VALUE_PARAMETERS_SHORT_NAMES) {
            return this
        }
        val jvmDescriptor = this.computeJvmDescriptorReplacingKotlinToJava()
        if (SpecialGenericSignatures.ERASED_VALUE_PARAMETERS_SIGNATURES.none { it.endsWith(jvmDescriptor) }) {
            return this
        }
        val superClassIds = listOfNotNull(symbol.callableId.classId) +
                lookupSuperTypes(owner, lookupInterfaces = true, deep = true, useSiteSession = session).map { it.lookupTag.classId }
        for (superClassId in superClassIds) {
            val javaClassId = JavaToKotlinClassMap.mapKotlinToJava(superClassId.asSingleFqName().toUnsafe()) ?: superClassId
            val fqJvmDescriptor = "${javaClassId.asString()}.$jvmDescriptor"
            if (fqJvmDescriptor in SpecialGenericSignatures.ERASED_VALUE_PARAMETERS_SIGNATURES) {
                val specialSignatureInfo = SpecialGenericSignatures.getSpecialSignatureInfo(fqJvmDescriptor)
                if (!specialSignatureInfo.isObjectReplacedWithTypeParameter) {
                    return this
                }
                val newParameterTypes = valueParameters.mapIndexed { i, valueParameter ->
                    val classLikeType =
                        valueParameter.returnTypeRef.coneTypeSafe<ConeKotlinType>()?.lowerBoundIfFlexible().safeAs<ConeClassLikeType>()
                    if (classLikeType?.lookupTag?.classId == StandardClassIds.Any) {
                        val typeParameterIndex = if (name.asString() == "containsValue") 1 else i
                        val typeParameter = typeParameters.getOrNull(typeParameterIndex) ?: typeParameters.first()
                        val type = ConeTypeParameterLookupTag(typeParameter.symbol).constructType(
                            emptyArray(), valueParameter.returnTypeRef.isMarkedNullable == true
                        )
                        if (valueParameter.returnTypeRef.coneType is ConeFlexibleType) {
                            ConeFlexibleType(type, type.withNullability(ConeNullability.NULLABLE))
                        } else {
                            type
                        }
                    } else {
                        null
                    }
                }
                if (newParameterTypes.none { it != null }) {
                    return this
                }

                return FirFakeOverrideGenerator.createCopyForFirFunction(
                    FirNamedFunctionSymbol(symbol.callableId),
                    this,
                    session,
                    FirDeclarationOrigin.Enhancement,
                    newParameterTypes = valueParameters.zip(newParameterTypes).map { (valueParameter, newType) ->
                        newType ?: valueParameter.returnTypeRef.coneType
                    },
                    newDispatchReceiverType = dispatchReceiverType,
                )
            }
        }
        return this
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        useSiteMemberScope.processFunctionsByName(name) process@{ original ->
            val symbol = signatureEnhancement.enhancedFunction(original, name)
            val enhancedFunction = (symbol.fir as? FirSimpleFunction)?.changeSignatureIfErasedValueParameter()
            val enhancedFunctionSymbol = enhancedFunction?.symbol ?: symbol

            overriddenFunctions[enhancedFunctionSymbol] =
                enhancedFunction
                    ?.overriddenMembers(enhancedFunction.name)
                    ?.mapNotNull { it.symbol as? FirFunctionSymbol<*> }
                    .orEmpty()

            processor(enhancedFunctionSymbol)
        }

        return super.processFunctionsByName(name, processor)
    }

    private fun FirCallableMemberDeclaration<*>.overriddenMembers(name: Name): List<FirCallableMemberDeclaration<*>> {
        val backMap = overrideBindCache.getOrPut(name) {
            useSiteMemberScope.bindOverrides(name)
            useSiteMemberScope
                .overrideByBase
                .toList()
                .groupBy({ (_, key) -> key }, { (value) -> value })
        }
        return backMap[this.symbol]?.map { it.fir as FirCallableMemberDeclaration<*> } ?: emptyList()
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        useSiteMemberScope.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        useSiteMemberScope.processDeclaredConstructors process@{ original ->
            val function = signatureEnhancement.enhancedFunction(original, name = null)
            processor(function as FirConstructorSymbol)
        }
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>, FirTypeScope) -> ProcessorAction
    ): ProcessorAction =
        doProcessDirectOverriddenCallables(
            functionSymbol, processor, overriddenFunctions, useSiteMemberScope,
            FirTypeScope::processDirectOverriddenFunctionsWithBaseScope
        )

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction = doProcessDirectOverriddenCallables(
        propertySymbol, processor, overriddenProperties, useSiteMemberScope,
        FirTypeScope::processDirectOverriddenPropertiesWithBaseScope
    )

    override fun getCallableNames(): Set<Name> {
        return useSiteMemberScope.getCallableNames()
    }

    override fun getClassifierNames(): Set<Name> {
        return useSiteMemberScope.getClassifierNames()
    }

    override fun mayContainName(name: Name): Boolean {
        return useSiteMemberScope.mayContainName(name)
    }
}
