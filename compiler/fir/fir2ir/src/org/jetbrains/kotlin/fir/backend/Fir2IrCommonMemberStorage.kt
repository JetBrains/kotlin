/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.backend.Fir2IrDeclarationStorage.PropertyCacheStorage.SyntheticPropertyKey
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeLookupTag
import org.jetbrains.kotlin.ir.IrLock
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.*
import java.util.concurrent.ConcurrentHashMap

/**
 * This storage contains the shared state of FIR2IR.
 *
 * State is shared between the conversions of different modules in the same platform compilation.
 *
 * See `/docs/fir/k2_kmp.md`
 */
class Fir2IrCommonMemberStorage {
    val lock: IrLock = IrLock()

    val classCache: MutableMap<FirRegularClass, IrClassSymbol> = mutableMapOf()
    val notFoundClassCache: ConcurrentHashMap<ConeClassLikeLookupTag, IrClass> = ConcurrentHashMap()

    val typeParameterCache: MutableMap<FirTypeParameter, IrTypeParameter> = mutableMapOf()

    val enumEntryCache: MutableMap<FirEnumEntry, IrEnumEntrySymbol> = mutableMapOf()

    val localClassCache: MutableMap<FirClass, IrClass> = mutableMapOf()
    val localCallableCache: MutableList<Fir2IrScopeCache> = mutableListOf()

    val functionCache: ConcurrentHashMap<FirFunction, IrSimpleFunctionSymbol> = ConcurrentHashMap()
    val dataClassGeneratedFunctionsCache: ConcurrentHashMap<FirClass, Fir2IrDeclarationStorage.DataClassGeneratedFunctionsStorage> =
        ConcurrentHashMap()

    val constructorCache: ConcurrentHashMap<FirConstructor, IrConstructorSymbol> = ConcurrentHashMap()

    val propertyCache: ConcurrentHashMap<FirProperty, IrPropertySymbol> = ConcurrentHashMap()
    val syntheticPropertyCache: ConcurrentHashMap<SyntheticPropertyKey, IrPropertySymbol> = ConcurrentHashMap()
    val getterForPropertyCache: ConcurrentHashMap<IrSymbol, IrSimpleFunctionSymbol> = ConcurrentHashMap()
    val setterForPropertyCache: ConcurrentHashMap<IrSymbol, IrSimpleFunctionSymbol> = ConcurrentHashMap()
    val backingFieldForPropertyCache: ConcurrentHashMap<IrPropertySymbol, IrFieldSymbol> = ConcurrentHashMap()
    val propertyForBackingFieldCache: ConcurrentHashMap<IrFieldSymbol, IrPropertySymbol> = ConcurrentHashMap()
    val delegateVariableForPropertyCache: ConcurrentHashMap<IrLocalDelegatedPropertySymbol, IrVariableSymbol> = ConcurrentHashMap()

    val irForFirSessionDependantDeclarationMap: MutableMap<Fir2IrDeclarationStorage.FakeOverrideIdentifier, IrSymbol> = mutableMapOf()

    /**
     * This map contains information about classes, which implement interfaces by delegation
     *
     * ```
     * class Some(val a: A, b: B) : A by a, B by b
     * ```
     *
     * delegatedClassesMap = {
     *     Some -> {
     *         A -> backingField of val a,
     *         B -> field for delegate b
     *     }
     * }
     */
    val delegatedClassesInfo: MutableMap<IrClassSymbol, MutableMap<IrClassSymbol, IrFieldSymbol>> = mutableMapOf()
    val firClassesWithInheritanceByDelegation: MutableSet<FirClass> = mutableSetOf()

    /**
     * Contains information about synthetic methods generated for data and value classes
     * It will be used to generate bodies of those methods after fir2ir conversion is over
     */
    val generatedDataValueClassSyntheticFunctions: MutableMap<IrClass, DataValueClassGeneratedMembersInfo> = mutableMapOf()

    data class DataValueClassGeneratedMembersInfo(
        val components: Fir2IrComponents,
        val firClass: FirRegularClass,
        val origin: IrDeclarationOrigin,
        val generatedFunctions: MutableList<IrSimpleFunction>
    )
}
