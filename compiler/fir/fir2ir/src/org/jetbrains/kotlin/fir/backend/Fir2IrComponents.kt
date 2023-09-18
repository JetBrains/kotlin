/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.generators.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.signaturer.FirBasedSignatureComposer
import org.jetbrains.kotlin.ir.IrLock
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.overrides.IrFakeOverrideBuilder
import org.jetbrains.kotlin.ir.util.SymbolTable

interface Fir2IrComponents {
    val session: FirSession
    val scopeSession: ScopeSession

    val converter: Fir2IrConverter

    val symbolTable: SymbolTable
    val irBuiltIns: IrBuiltInsOverFir
    val builtIns: Fir2IrBuiltIns
    val irFactory: IrFactory
    val irProviders: List<IrProvider>
    val lock: IrLock

    val classifierStorage: Fir2IrClassifierStorage
    val declarationStorage: Fir2IrDeclarationStorage

    val typeConverter: Fir2IrTypeConverter
    val signatureComposer: FirBasedSignatureComposer
    val visibilityConverter: Fir2IrVisibilityConverter

    val callablesGenerator: Fir2IrCallableDeclarationsGenerator
    val classifiersGenerator: Fir2IrClassifiersGenerator
    val lazyDeclarationsGenerator: Fir2IrLazyDeclarationsGenerator

    val annotationGenerator: AnnotationGenerator
    val callGenerator: CallAndReferenceGenerator
    val fakeOverrideGenerator: FakeOverrideGenerator
    val delegatedMemberGenerator: DelegatedMemberGenerator
    val fakeOverrideBuilder: IrFakeOverrideBuilder

    val extensions: Fir2IrExtensions
    val configuration: Fir2IrConfiguration

    val annotationsFromPluginRegistrar: Fir2IrAnnotationsFromPluginRegistrar
}
