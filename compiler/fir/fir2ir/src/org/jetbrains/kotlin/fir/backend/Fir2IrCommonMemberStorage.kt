/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.signaturer.FirBasedSignatureComposer
import org.jetbrains.kotlin.fir.signaturer.FirMangler
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.IdSignatureComposer
import org.jetbrains.kotlin.ir.util.SymbolTable
import java.util.concurrent.ConcurrentHashMap

class Fir2IrCommonMemberStorage(
    generateSignatures: Boolean,
    signatureComposerCreator: (() -> IdSignatureComposer)?,
    manglerCreator: () -> FirMangler
) {
    val signatureComposer: FirBasedSignatureComposer

    val symbolTable: SymbolTable

    init {
        val signaturer = if (generateSignatures && signatureComposerCreator != null)
            signatureComposerCreator()
        else
            DescriptorSignatureComposerStub()
        signatureComposer = FirBasedSignatureComposer(manglerCreator())
        symbolTable = SymbolTable(
            signaturer = WrappedDescriptorSignatureComposer(signaturer, signatureComposer),
            irFactory = IrFactoryImpl
        )
    }

    val classCache: MutableMap<FirRegularClass, IrClass> = mutableMapOf()

    val typeParameterCache: MutableMap<FirTypeParameter, IrTypeParameter> = mutableMapOf()

    val enumEntryCache: MutableMap<FirEnumEntry, IrEnumEntry> = mutableMapOf()

    val localClassCache: MutableMap<FirClass, IrClass> = mutableMapOf()

    val functionCache: ConcurrentHashMap<FirFunction, IrSimpleFunction> = ConcurrentHashMap()

    val constructorCache: ConcurrentHashMap<FirConstructor, IrConstructor> = ConcurrentHashMap()

    val propertyCache: ConcurrentHashMap<FirProperty, IrProperty> = ConcurrentHashMap()

    val fakeOverridesInClass: MutableMap<IrClass, MutableMap<FirCallableDeclaration, FirCallableDeclaration>> = mutableMapOf()
}