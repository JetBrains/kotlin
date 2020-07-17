/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.generators.CallAndReferenceGenerator
import org.jetbrains.kotlin.fir.backend.generators.FakeOverrideGenerator
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.SymbolTable

interface Fir2IrComponents {
    val session: FirSession
    val scopeSession: ScopeSession
    val symbolTable: SymbolTable
    val irBuiltIns: IrBuiltIns
    val classifierStorage: Fir2IrClassifierStorage
    val declarationStorage: Fir2IrDeclarationStorage
    val typeConverter: Fir2IrTypeConverter
    val signatureComposer: Fir2IrSignatureComposer
    val callGenerator: CallAndReferenceGenerator
    val fakeOverrideGenerator: FakeOverrideGenerator
}