/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.generators.CallAndReferenceGenerator
import org.jetbrains.kotlin.fir.backend.generators.FakeOverrideGenerator
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.signaturer.FirBasedSignatureComposer
import org.jetbrains.kotlin.fir.signaturer.FirMangler
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.SymbolTable

class Fir2IrComponentsStorage(
    override val session: FirSession,
    override val scopeSession: ScopeSession,
    override val symbolTable: SymbolTable,
    override val irBuiltIns: IrBuiltIns,
    mangler: FirMangler
) : Fir2IrComponents {
    override lateinit var classifierStorage: Fir2IrClassifierStorage
    override lateinit var declarationStorage: Fir2IrDeclarationStorage
    override lateinit var typeConverter: Fir2IrTypeConverter
    override lateinit var callGenerator: CallAndReferenceGenerator
    override lateinit var fakeOverrideGenerator: FakeOverrideGenerator

    override val signatureComposer = FirBasedSignatureComposer(mangler)
}