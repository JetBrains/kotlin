/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

class FirDelegatedMemberScope(
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    private val containingClass: FirClass,
    private val declaredMemberScope: FirScope,
    private val delegateFields: List<FirField>,
) : FirScope(), FirContainingNamesAwareScope {
    private val dispatchReceiverType = containingClass.defaultType()
    private val overrideChecker = FirStandardOverrideChecker(session)

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        declaredMemberScope.processFunctionsByName(name, processor)
        val result = mutableListOf<FirNamedFunctionSymbol>()

        for (delegateField in delegateFields) {
            collectFunctionsFromSpecificField(delegateField, name, result)
        }

        result.forEach(processor)
    }

    private fun buildScope(delegateField: FirField): FirTypeScope? {
        delegateField.ensureResolved(FirResolvePhase.TYPES, session)
        return delegateField.returnTypeRef.coneType.scope(session, scopeSession, FakeOverrideTypeCalculator.DoNothing)
    }

    private fun collectFunctionsFromSpecificField(
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
            collectPropertiesFromSpecificField(delegateField, name, result)
        }

        result.forEach(processor)
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        declaredMemberScope.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        declaredMemberScope.processDeclaredConstructors(processor)
    }

    private fun collectPropertiesFromSpecificField(
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

    @OptIn(ExperimentalStdlibApi::class)
    private val callableNamesLazy: Set<Name> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildSet {
            addAll(declaredMemberScope.getContainingCallableNamesIfPresent())

            delegateFields.flatMapTo(this) {
                buildScope(it)?.getCallableNames() ?: emptySet()
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private val classifierNamesLazy: Set<Name> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildSet {
            addAll(declaredMemberScope.getContainingClassifierNamesIfPresent())

            delegateFields.flatMapTo(this) {
                buildScope(it)?.getClassifierNames() ?: emptySet()
            }
        }
    }

    override fun getCallableNames(): Set<Name> = callableNamesLazy
    override fun getClassifierNames(): Set<Name> = classifierNamesLazy
}

private object MultipleDelegatesWithTheSameSignatureKey : FirDeclarationDataKey()

var FirCallableDeclaration.multipleDelegatesWithTheSameSignature: Boolean? by FirDeclarationDataRegistry.data(MultipleDelegatesWithTheSameSignatureKey)

private object DelegatedWrapperDataKey : FirDeclarationDataKey()
class DelegatedWrapperData<D : FirCallableDeclaration>(
    val wrapped: D,
    val containingClass: ConeClassLikeLookupTag,
    val delegateField: FirField,
)
var <D : FirCallableDeclaration>
        D.delegatedWrapperData: DelegatedWrapperData<D>? by FirDeclarationDataRegistry.data(DelegatedWrapperDataKey)


// From the definition of function interfaces in the Java specification (pt. 9.8):
// "methods that are members of I that do not have the same signature as any public instance method of the class Object"
// It means that if an interface declares `int hashCode()` then the method won't be taken into account when
// checking if the interface is SAM.
fun FirSimpleFunction.isPublicInAny(): Boolean {
    if (name.asString() !in PUBLIC_METHOD_NAMES_IN_ANY) return false

    return when (name.asString()) {
        "hashCode", "toString" -> valueParameters.isEmpty()
        "equals" -> valueParameters.singleOrNull()?.hasTypeOf(StandardClassIds.Any, allowNullable = true) == true
        else -> error("Unexpected method name: $name")
    }
}

fun FirValueParameter.hasTypeOf(classId: ClassId, allowNullable: Boolean): Boolean {
    val classLike = when (val type = returnTypeRef.coneType) {
        is ConeClassLikeType -> type
        is ConeFlexibleType -> type.upperBound as? ConeClassLikeType ?: return false
        else -> return false
    }

    if (classLike.isMarkedNullable && !allowNullable) return false
    return classLike.lookupTag.classId == classId
}

private val PUBLIC_METHOD_NAMES_IN_ANY = setOf("equals", "hashCode", "toString")
