/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.generators.*
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.signaturer.FirBasedSignatureComposer
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrLock
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.overrides.IrFakeOverrideBuilder
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.SymbolTable

class Fir2IrComponentsStorage(
    override val session: FirSession,
    override val scopeSession: ScopeSession,
    override val irFactory: IrFactory,
    override val extensions: Fir2IrExtensions,
    override val configuration: Fir2IrConfiguration,
    override val visibilityConverter: Fir2IrVisibilityConverter,
    irFakeOverrideBuilderProvider: (IrBuiltIns) -> IrFakeOverrideBuilder,
    moduleDescriptor: FirModuleDescriptor,
    commonMemberStorage: Fir2IrCommonMemberStorage,
    irMangler: KotlinMangler.IrMangler,
    specialSymbolProvider: Fir2IrSpecialSymbolProvider,
    initializedIrBuiltIns: IrBuiltInsOverFir?
) : Fir2IrComponents {
    override val firProvider: FirProviderWithGeneratedFiles = FirProviderWithGeneratedFiles(
        session,
        commonMemberStorage.previousFirProviders
    )

    override val signatureComposer: FirBasedSignatureComposer = commonMemberStorage.firSignatureComposer
    override val symbolTable: SymbolTable = commonMemberStorage.symbolTable

    private val conversionScope = Fir2IrConversionScope(configuration)

    override val converter: Fir2IrConverter = Fir2IrConverter(moduleDescriptor, this, conversionScope)

    override val classifierStorage: Fir2IrClassifierStorage = Fir2IrClassifierStorage(this, commonMemberStorage, conversionScope)
    override val declarationStorage: Fir2IrDeclarationStorage = Fir2IrDeclarationStorage(this, moduleDescriptor, commonMemberStorage)

    override val callablesGenerator: Fir2IrCallableDeclarationsGenerator = Fir2IrCallableDeclarationsGenerator(this)
    override val classifiersGenerator: Fir2IrClassifiersGenerator = Fir2IrClassifiersGenerator(this)
    override val lazyDeclarationsGenerator: Fir2IrLazyDeclarationsGenerator = Fir2IrLazyDeclarationsGenerator(this)

    // builtins should go after storages and generators, because they use them during initialization
    override val irBuiltIns: IrBuiltInsOverFir = initializedIrBuiltIns ?: IrBuiltInsOverFir(
        this, configuration.languageVersionSettings, moduleDescriptor, irMangler
    )
    override val builtIns: Fir2IrBuiltIns = Fir2IrBuiltIns(this, specialSymbolProvider)
    override val fakeOverrideBuilder: IrFakeOverrideBuilder = irFakeOverrideBuilderProvider(irBuiltIns)

    override val irProviders: List<IrProvider> = listOf(FirIrProvider(this))

    override val typeConverter: Fir2IrTypeConverter = Fir2IrTypeConverter(this, conversionScope)

    val fir2IrVisitor: Fir2IrVisitor = Fir2IrVisitor(this, conversionScope)

    override val annotationGenerator: AnnotationGenerator = AnnotationGenerator(this)
    override val callGenerator: CallAndReferenceGenerator = CallAndReferenceGenerator(this, fir2IrVisitor, conversionScope)
    @FirBasedFakeOverrideGenerator
    override val fakeOverrideGenerator: FakeOverrideGenerator = FakeOverrideGenerator(this, conversionScope)
    override val delegatedMemberGenerator: DelegatedMemberGenerator = DelegatedMemberGenerator(this)
    override val symbolsMappingForLazyClasses: Fir2IrSymbolsMappingForLazyClasses = Fir2IrSymbolsMappingForLazyClasses()

    override val annotationsFromPluginRegistrar: Fir2IrIrGeneratedDeclarationsRegistrar = Fir2IrIrGeneratedDeclarationsRegistrar(this)

    override val lock: IrLock
        get() = symbolTable.lock

    override val manglers: Fir2IrComponents.Manglers = object : Fir2IrComponents.Manglers {
        override val irMangler: KotlinMangler.IrMangler
            get() = irMangler

        override val firMangler: FirMangler
            get() = commonMemberStorage.firSignatureComposer.mangler
    }
}
