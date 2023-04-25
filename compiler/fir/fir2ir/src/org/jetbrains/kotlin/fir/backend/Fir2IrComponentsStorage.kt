/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.generators.AnnotationGenerator
import org.jetbrains.kotlin.fir.backend.generators.CallAndReferenceGenerator
import org.jetbrains.kotlin.fir.backend.generators.DelegatedMemberGenerator
import org.jetbrains.kotlin.fir.backend.generators.FakeOverrideGenerator
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.signaturer.FirBasedSignatureComposer
import org.jetbrains.kotlin.ir.IrLock
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.util.SymbolTable

class Fir2IrComponentsStorage(
    override val session: FirSession,
    override val scopeSession: ScopeSession,
    override val symbolTable: SymbolTable,
    override val irFactory: IrFactory,
    override val signatureComposer: FirBasedSignatureComposer,
    override val extensions: Fir2IrExtensions,
    override val configuration: Fir2IrConfiguration,
) : Fir2IrComponents {

    override lateinit var converter: Fir2IrConverter

    override lateinit var classifierStorage: Fir2IrClassifierStorage
    override lateinit var declarationStorage: Fir2IrDeclarationStorage

    override lateinit var irBuiltIns: IrBuiltInsOverFir
    override lateinit var builtIns: Fir2IrBuiltIns
    override lateinit var irProviders: List<IrProvider>

    override lateinit var typeConverter: Fir2IrTypeConverter
    override lateinit var visibilityConverter: Fir2IrVisibilityConverter

    override lateinit var annotationGenerator: AnnotationGenerator
    override lateinit var callGenerator: CallAndReferenceGenerator
    override lateinit var fakeOverrideGenerator: FakeOverrideGenerator
    override lateinit var delegatedMemberGenerator: DelegatedMemberGenerator

    override val lock: IrLock
        get() = symbolTable.lock
}
