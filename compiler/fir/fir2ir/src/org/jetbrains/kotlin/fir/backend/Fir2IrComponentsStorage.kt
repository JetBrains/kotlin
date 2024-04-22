/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.IrSpecialAnnotationsProvider
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.generators.*
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.signaturer.FirBasedSignatureComposer
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrLock
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.overrides.IrFakeOverrideBuilder
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class Fir2IrComponentsStorage(
    override val session: FirSession,
    override val scopeSession: ScopeSession,
    val fir: List<FirFile>,
    override val irFactory: IrFactory,
    override val extensions: Fir2IrExtensions,
    override val configuration: Fir2IrConfiguration,
    override val visibilityConverter: Fir2IrVisibilityConverter,
    actualizerTypeContextProvider: (IrBuiltIns) -> IrTypeSystemContext,
    commonMemberStorage: Fir2IrCommonMemberStorage,
    irMangler: KotlinMangler.IrMangler,
    kotlinBuiltIns: KotlinBuiltIns,
    initializedIrBuiltIns: IrBuiltInsOverFir?,
    override val specialAnnotationsProvider: IrSpecialAnnotationsProvider?,
    initializedIrTypeSystemContext: IrTypeSystemContext?,
    override val firProvider: FirProviderWithGeneratedFiles,
) : Fir2IrComponents {
    override val filesBeingCompiled: Set<FirFile>? = runIf(configuration.allowNonCachedDeclarations) { fir.toSet() }

    val moduleDescriptor: FirModuleDescriptor = FirModuleDescriptor.createSourceModuleDescriptor(session, kotlinBuiltIns)

    override val signatureComposer: FirBasedSignatureComposer = commonMemberStorage.firSignatureComposer
    override val symbolTable: SymbolTable = commonMemberStorage.symbolTable

    private val conversionScope = Fir2IrConversionScope(configuration)

    override val converter: Fir2IrConverter = Fir2IrConverter(moduleDescriptor, this, conversionScope)

    override val classifierStorage: Fir2IrClassifierStorage = Fir2IrClassifierStorage(this, commonMemberStorage, conversionScope)
    override val declarationStorage: Fir2IrDeclarationStorage = Fir2IrDeclarationStorage(this, moduleDescriptor, commonMemberStorage)

    override val callablesGenerator: Fir2IrCallableDeclarationsGenerator = Fir2IrCallableDeclarationsGenerator(this)
    override val classifiersGenerator: Fir2IrClassifiersGenerator = Fir2IrClassifiersGenerator(this)
    override val lazyDeclarationsGenerator: Fir2IrLazyDeclarationsGenerator = Fir2IrLazyDeclarationsGenerator(this)
    override val dataClassMembersGenerator: Fir2IrDataClassMembersGenerator = Fir2IrDataClassMembersGenerator(this)

    // builtins should go after storages and generators, because they use them during initialization
    override val irBuiltIns: IrBuiltInsOverFir = initializedIrBuiltIns ?: IrBuiltInsOverFir(
        this, configuration.languageVersionSettings, moduleDescriptor, irMangler
    )
    val irTypeSystemContext: IrTypeSystemContext = initializedIrTypeSystemContext ?: actualizerTypeContextProvider(irBuiltIns)

    override val fakeOverrideBuilder: IrFakeOverrideBuilder = IrFakeOverrideBuilder(
        irTypeSystemContext,
        Fir2IrFakeOverrideStrategy(
            Fir2IrConverter.friendModulesMap(session),
            isGenericClashFromSameSupertypeAllowed = session.moduleData.platform.isJvm(),
            isOverrideOfPublishedApiFromOtherModuleDisallowed = session.moduleData.platform.isJvm(),
        ),
        extensions.externalOverridabilityConditions
    )

    override val irProviders: List<IrProvider> = emptyList()

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
