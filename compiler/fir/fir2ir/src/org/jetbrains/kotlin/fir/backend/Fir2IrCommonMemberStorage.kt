/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.signaturer.FirBasedSignatureComposer
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.SymbolTable
import java.util.concurrent.ConcurrentHashMap

/**
 * This storage contains the shared state of FIR2IR.
 *
 * State is shared between the conversions of different modules in the same platform compilation.
 *
 * See `/docs/fir/k2_kmp.md`
 */
class Fir2IrCommonMemberStorage(firMangler: FirMangler) {
    val firSignatureComposer = FirBasedSignatureComposer(firMangler)

    val symbolTable = SymbolTable(signaturer = null, irFactory = IrFactoryImpl)

    val classCache: MutableMap<FirRegularClass, IrClass> = mutableMapOf()

    val typeParameterCache: MutableMap<FirTypeParameter, IrTypeParameter> = mutableMapOf()

    val enumEntryCache: MutableMap<FirEnumEntry, IrEnumEntry> = mutableMapOf()

    val localClassCache: MutableMap<FirClass, IrClass> = mutableMapOf()

    val functionCache: ConcurrentHashMap<FirFunction, IrSimpleFunctionSymbol> = ConcurrentHashMap()

    val constructorCache: ConcurrentHashMap<FirConstructor, IrConstructorSymbol> = ConcurrentHashMap()

    val fieldCache: ConcurrentHashMap<FirField, IrFieldSymbol> = ConcurrentHashMap()

    val propertyCache: ConcurrentHashMap<FirProperty, IrPropertySymbol> = ConcurrentHashMap()
    val syntheticPropertyCache: ConcurrentHashMap<FirFunction, IrPropertySymbol> = ConcurrentHashMap()
    val getterForPropertyCache: ConcurrentHashMap<IrSymbol, IrSimpleFunctionSymbol> = ConcurrentHashMap()
    val setterForPropertyCache: ConcurrentHashMap<IrSymbol, IrSimpleFunctionSymbol> = ConcurrentHashMap()
    val backingFieldForPropertyCache: ConcurrentHashMap<IrPropertySymbol, IrFieldSymbol> = ConcurrentHashMap()
    val propertyForBackingFieldCache: ConcurrentHashMap<IrFieldSymbol, IrPropertySymbol> = ConcurrentHashMap()
    val delegateVariableForPropertyCache: ConcurrentHashMap<IrLocalDelegatedPropertySymbol, IrVariableSymbol> = ConcurrentHashMap()

    val fakeOverridesInClass: MutableMap<IrClass, MutableMap<Fir2IrDeclarationStorage.FirOverrideKey, FirCallableDeclaration>> = mutableMapOf()

    val irForFirSessionDependantDeclarationMap: MutableMap<Fir2IrDeclarationStorage.FakeOverrideIdentifier, IrSymbol> = mutableMapOf()

    fun registerFirProvider(moduleData: FirModuleData, firProvider: FirProviderWithGeneratedFiles) {
        require(moduleData !in _previousFirProviders) { "FirProvider for $moduleData already registered"}
        _previousFirProviders[moduleData] = firProvider
    }

    val previousFirProviders: Map<FirModuleData, FirProviderWithGeneratedFiles>
        get() = _previousFirProviders

    private val _previousFirProviders: MutableMap<FirModuleData, FirProviderWithGeneratedFiles> = mutableMapOf()
}
