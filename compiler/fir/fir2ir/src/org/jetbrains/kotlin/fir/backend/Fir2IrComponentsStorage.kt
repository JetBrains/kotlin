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
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.signaturer.FirBasedSignatureComposer
import org.jetbrains.kotlin.ir.IrLock
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.SymbolTable

class Fir2IrComponentsStorage(
    override val session: FirSession,
    override val scopeSession: ScopeSession,
    override val irFactory: IrFactory,
    override val extensions: Fir2IrExtensions,
    override val configuration: Fir2IrConfiguration,
    override val visibilityConverter: Fir2IrVisibilityConverter,
    moduleDescriptor: FirModuleDescriptor,
    commonMemberStorage: Fir2IrCommonMemberStorage,
    irMangler: KotlinMangler.IrMangler,
    specialSymbolProvider: Fir2IrSpecialSymbolProvider,
    initializedIrBuiltIns: IrBuiltInsOverFir?
) : Fir2IrComponents {
    override val signatureComposer: FirBasedSignatureComposer = commonMemberStorage.firSignatureComposer
    override val symbolTable: SymbolTable = commonMemberStorage.symbolTable

    override val converter: Fir2IrConverter = Fir2IrConverter(moduleDescriptor, this)

    override val classifierStorage: Fir2IrClassifierStorage = Fir2IrClassifierStorage(this, commonMemberStorage)
    override val declarationStorage: Fir2IrDeclarationStorage = Fir2IrDeclarationStorage(this, moduleDescriptor, commonMemberStorage)

    override val irBuiltIns: IrBuiltInsOverFir = initializedIrBuiltIns ?: IrBuiltInsOverFir(
        this, configuration.languageVersionSettings, moduleDescriptor, irMangler
    )

    override val builtIns: Fir2IrBuiltIns = Fir2IrBuiltIns(this, specialSymbolProvider)
    override val irProviders: List<IrProvider> = listOf(FirIrProvider(this))

    override val typeConverter: Fir2IrTypeConverter = Fir2IrTypeConverter(this)

    private val conversionScope = Fir2IrConversionScope(configuration)
    val fir2IrVisitor: Fir2IrVisitor = Fir2IrVisitor(this, conversionScope)

    override val annotationGenerator: AnnotationGenerator = AnnotationGenerator(this)
    override val callGenerator: CallAndReferenceGenerator = CallAndReferenceGenerator(this, fir2IrVisitor, conversionScope)
    override val fakeOverrideGenerator: FakeOverrideGenerator = FakeOverrideGenerator(this, conversionScope)
    override val delegatedMemberGenerator: DelegatedMemberGenerator = DelegatedMemberGenerator(this)

    override val annotationsFromPluginRegistrar: Fir2IrAnnotationsFromPluginRegistrar = Fir2IrAnnotationsFromPluginRegistrar(this)

    override val lock: IrLock
        get() = symbolTable.lock
}
