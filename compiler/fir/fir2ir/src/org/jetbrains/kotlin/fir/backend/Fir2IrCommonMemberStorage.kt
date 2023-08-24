/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.signaturer.FirBasedSignatureComposer
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignatureComposer
import org.jetbrains.kotlin.ir.util.SymbolTable
import java.util.concurrent.ConcurrentHashMap

class Fir2IrCommonMemberStorage(
    signatureComposer: IdSignatureComposer,
    firMangler: FirMangler
) {
    val firSignatureComposer = FirBasedSignatureComposer(firMangler)

    val symbolTable = SymbolTable(signaturer = signatureComposer, irFactory = IrFactoryImpl)

    val classCache: MutableMap<FirRegularClass, IrClass> = mutableMapOf()

    val typeParameterCache: MutableMap<FirTypeParameter, IrTypeParameter> = mutableMapOf()

    val enumEntryCache: MutableMap<FirEnumEntry, IrEnumEntry> = mutableMapOf()

    val localClassCache: MutableMap<FirClass, IrClass> = mutableMapOf()

    val functionCache: ConcurrentHashMap<FirFunction, IrSimpleFunction> = ConcurrentHashMap()

    val constructorCache: ConcurrentHashMap<FirConstructor, IrConstructor> = ConcurrentHashMap()

    val propertyCache: ConcurrentHashMap<FirProperty, IrProperty> = ConcurrentHashMap()
    val getterForPropertyCache: ConcurrentHashMap<IrSymbol, IrSimpleFunctionSymbol> = ConcurrentHashMap()
    val setterForPropertyCache: ConcurrentHashMap<IrSymbol, IrSimpleFunctionSymbol> = ConcurrentHashMap()
    val backingFieldForPropertyCache: ConcurrentHashMap<IrPropertySymbol, IrFieldSymbol> = ConcurrentHashMap()
    val propertyForBackingFieldCache: ConcurrentHashMap<IrFieldSymbol, IrPropertySymbol> = ConcurrentHashMap()
    val delegateVariableForPropertyCache: ConcurrentHashMap<IrLocalDelegatedPropertySymbol, IrVariableSymbol> = ConcurrentHashMap()

    val fakeOverridesInClass: MutableMap<IrClass, MutableMap<Fir2IrDeclarationStorage.FakeOverrideKey, FirCallableDeclaration>> = mutableMapOf()

    val irFakeOverridesForFirFakeOverrideMap: MutableMap<Fir2IrDeclarationStorage.FakeOverrideIdentifier, IrDeclaration> = mutableMapOf()
}
