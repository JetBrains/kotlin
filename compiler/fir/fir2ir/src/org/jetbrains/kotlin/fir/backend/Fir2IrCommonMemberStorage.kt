/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.backend.Fir2IrDeclarationStorage.PropertyCacheStorage.SyntheticPropertyKey
import org.jetbrains.kotlin.fir.backend.utils.filterOutSymbolsFromCache
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
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

    // Should be updated respectively when adding new properties
    fun cloneFilteringSymbols(filterOutSymbols: Set<FirBasedSymbol<*>>): Fir2IrCommonMemberStorage {
        val original = this
        return Fir2IrCommonMemberStorage().apply {
            classCache.putAll(filterOutSymbolsFromCache(original.classCache, filterOutSymbols))
            notFoundClassCache.putAll(original.notFoundClassCache)
            typeParameterCache.putAll(filterOutSymbolsFromCache(original.typeParameterCache, filterOutSymbols))
            enumEntryCache.putAll(filterOutSymbolsFromCache(original.enumEntryCache, filterOutSymbols))
            localClassCache.putAll(filterOutSymbolsFromCache(original.localClassCache, filterOutSymbols))
            localCallableCache.addAll(original.localCallableCache.map { it.cloneFilteringSymbols(filterOutSymbols) })
            functionCache.putAll(filterOutSymbolsFromCache(original.functionCache, filterOutSymbols))
            dataClassGeneratedFunctionsCache.putAll(filterOutSymbolsFromCache(original.dataClassGeneratedFunctionsCache, filterOutSymbols))
            constructorCache.putAll(filterOutSymbolsFromCache(original.constructorCache, filterOutSymbols))
            propertyCache.putAll(filterOutSymbolsFromCache(original.propertyCache, filterOutSymbols))
            syntheticPropertyCache.putAll(original.syntheticPropertyCache.filterKeys { !filterOutSymbols.contains(it.originalFunction.symbol) })
            getterForPropertyCache.putAll(original.getterForPropertyCache)
            setterForPropertyCache.putAll(original.setterForPropertyCache)
            backingFieldForPropertyCache.putAll(original.backingFieldForPropertyCache)
            delegateVariableForPropertyCache.putAll(original.delegateVariableForPropertyCache)
            irForFirSessionDependantDeclarationMap.putAll(original.irForFirSessionDependantDeclarationMap.filterKeys {
                !filterOutSymbols.contains(it.originalSymbol)
            })
            delegatedClassesInfo.putAll(original.delegatedClassesInfo)
            firClassesWithInheritanceByDelegation.addAll(original.firClassesWithInheritanceByDelegation.filter {
                !filterOutSymbols.contains(it.symbol)
            })
        }
    }

    data class DataValueClassGeneratedMembersInfo(
        val components: Fir2IrComponents,
        val firClass: FirRegularClass,
        val origin: IrDeclarationOrigin,
        val generatedFunctions: MutableList<IrSimpleFunction>
    )
}
