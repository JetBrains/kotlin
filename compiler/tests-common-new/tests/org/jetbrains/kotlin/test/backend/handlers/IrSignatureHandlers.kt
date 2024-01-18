/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.backend.common.CommonKLibResolver
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.fir.lazy.AbstractFir2IrLazyDeclaration
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.MetadataLibrary
import org.jetbrains.kotlin.library.metadata.kotlinLibrary
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler.Companion.groupWithTestFiles
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.RegisteredDirectivesBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.SKIP_SIGNATURE_VERIFICATION
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.ComposedRegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.UniqueRegisteredDirectives
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirModuleInfoProvider
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.util.DummyLogger
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

/**
 * Collects and memorizes signatures from IR nodes.
 *
 * To be executed between `frontendToBackend` and `backendFacade` steps.
 */
abstract class AbstractCollectAndMemorizeIdSignatures(
    testServices: TestServices,
    private val irMangler: KotlinMangler.IrMangler
) : AbstractIrHandler(testServices, BackendKinds.IrBackend) {
    final override fun processModule(module: TestModule, info: IrBackendInput) = Unit

    final override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (isSignatureVerificationSkipped) return

        val collector = if (arbitraryTestModule.frontendKind == FrontendKinds.FIR)
            SignatureCollector.createForK2(PublicIdSignatureComputer(irMangler))
        else
            SignatureCollector.createForK1(testServices)

        producedArtifactsMap.forEach { (module, irBackendInput) ->
            val irModuleFragment = irBackendInput?.irModuleFragment ?: return@forEach

            for (irFile in irModuleFragment.filteredIrFiles(module)) {
                for (declaration in irFile.declarations) {
                    collector.collectFrom(declaration)
                }
            }
        }

        testServices.signaturesDumpFile.writeText(collector.toString())
    }

    companion object {
        private fun IrModuleFragment.filteredIrFiles(module: TestModule): List<IrFile> = files
            .groupWithTestFiles(module)
            .mapNotNull { (testFile, irFile) ->
                irFile.takeUnless { testFile?.isAdditional == true }
            }
    }
}

abstract class AbstractVerifyIdSignaturesByKlib(
    testServices: TestServices,
) : KlibArtifactHandler(
    testServices,
    failureDisablesNextSteps = false,
    doNotRunIfThereWerePreviousFailures = false
) {
    final override fun processModule(module: TestModule, info: BinaryArtifacts.KLib) = Unit

    protected fun allProducedKlibs(): List<KotlinLibrary> = producedArtifactsMap.values.mapNotNull { klib ->
        val libraryPath = klib?.outputFile?.absolutePath ?: return@mapNotNull null

        CommonKLibResolver.resolveWithoutDependencies(
            libraries = listOf(libraryPath),
            logger = DummyLogger,
            zipAccessor = null
        ).libraries.single()
    }
}

abstract class AbstractDescriptorAwareVerifyIdSignaturesByKlib(
    testServices: TestServices,
    protected val descriptorMangler: KotlinMangler.DescriptorMangler,
) : AbstractVerifyIdSignaturesByKlib(testServices) {
    data class LoadedModules(
        val libraryModules: Collection<ModuleDescriptorImpl>,
        val additionalModules: Collection<ModuleDescriptorImpl>,
    ) {
        val builtIns: KotlinBuiltIns get() = libraryModules.first().builtIns
    }

    protected abstract fun loadModules(libraries: Collection<KotlinLibrary>): LoadedModules
    protected abstract val createIrLinker: (IrMessageLogger, IrBuiltIns, SymbolTable) -> KotlinIrLinker
    protected abstract fun extractDeclarations(loadedModules: LoadedModules, irLinker: KotlinIrLinker): Sequence<IrDeclaration>

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    final override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (isSignatureVerificationSkipped) return

        val libraries = allProducedKlibs()
        val loadedModules = loadModules(libraries)

        val symbolTable = SymbolTable(IdSignatureDescriptor(descriptorMangler), IrFactoryImpl)
        val typeTranslator = TypeTranslatorImpl(symbolTable, LanguageVersionSettingsImpl.DEFAULT, loadedModules.builtIns.builtInsModule)
        val irBuiltIns = IrBuiltInsOverDescriptors(loadedModules.builtIns, typeTranslator, symbolTable)
        val irMessageLogger = testServices.compilerConfigurationProvider.getCompilerConfiguration(arbitraryTestModule).irMessageLogger

        val irLinker = createIrLinker(irMessageLogger, irBuiltIns, symbolTable)

        val collector = SignatureCollector.createForK1(testServices)
        for (declaration in extractDeclarations(loadedModules, irLinker)) {
            collector.collectFrom(declaration)
        }

        assertions.assertEqualsToFile(
            expectedFile = testServices.signaturesDumpFile,
            actual = collector.toString()
        )
    }
}

/**
 * Reads KLIB, deserializes, collects signatures from IR nodes and compares them against the signatures
 * previously collected by [AbstractCollectAndMemorizeIdSignatures].
 *
 * To be executed after `backendFacade` step.
 */
abstract class AbstractVerifyIdSignaturesByDeserializedIr(
    testServices: TestServices,
    descriptorMangler: KotlinMangler.DescriptorMangler,
) : AbstractDescriptorAwareVerifyIdSignaturesByKlib(testServices, descriptorMangler) {
    final override fun extractDeclarations(loadedModules: LoadedModules, irLinker: KotlinIrLinker): Sequence<IrDeclaration> {
        loadedModules.additionalModules.forEach { it.toIrModule(irLinker, DeserializationStrategy.ONLY_DECLARATION_HEADERS) }

        val irModules = loadedModules.libraryModules.map {
            // Note: 'ALL' is the single strategy that automatically enqueues all IR files in the module for deserialization.
            it.toIrModule(irLinker, DeserializationStrategy.ALL)
        }

        ExternalDependenciesGenerator(irLinker.symbolTable, listOf(irLinker)).generateUnboundSymbolsAsDependencies()
        irLinker.fakeOverrideBuilder.provideFakeOverrides()

        return irModules.asSequence().flatMap(IrModuleFragment::files).flatMap(IrFile::declarations)
    }

    companion object {
        private fun ModuleDescriptor.toIrModule(irLinker: KotlinIrLinker, strategy: DeserializationStrategy): IrModuleFragment {
            val irModule = irLinker.deserializeIrModuleHeader(this, kotlinLibrary, { strategy })
            irLinker.resolveModuleDeserializer(this, null).init()
            return irModule
        }
    }
}

/**
 * Reads KLIB, builds descriptors on top of the deserialized metadata, builds LazyIr, collects signatures
 * from LazyIr nodes and compares them against the signatures previously collected by [AbstractCollectAndMemorizeIdSignatures].
 *
 * To be executed after `backendFacade` step.
 */
abstract class AbstractVerifyIdSignaturesByK1LazyIr(
    testServices: TestServices,
    descriptorMangler: KotlinMangler.DescriptorMangler,
) : AbstractDescriptorAwareVerifyIdSignaturesByKlib(testServices, descriptorMangler) {
    final override fun extractDeclarations(loadedModules: LoadedModules, irLinker: KotlinIrLinker): Sequence<IrDeclaration> =
        loadedModules.libraryModules.asSequence().flatMap { moduleDescriptor ->
            val stubGenerator = DeclarationStubGeneratorImpl(
                moduleDescriptor,
                irLinker.symbolTable,
                irLinker.builtIns,
                DescriptorByIdSignatureFinderImpl(moduleDescriptor, descriptorMangler)
            )

            moduleDescriptor.getAllPackagesFragments().flatMap { packageFragment ->
                packageFragment.getMemberScope().getContributedDescriptors().map(stubGenerator::generateMemberStub)
            }
        }

    companion object {
        private fun ModuleDescriptorImpl.getAllPackagesFragments(): Collection<PackageFragmentDescriptor> {
            val result = mutableListOf<PackageFragmentDescriptor>()
            val packageFragmentProvider = packageFragmentProviderForModuleContentWithoutDependencies

            fun getSubPackages(fqName: FqName) {
                result += packageFragmentProvider.packageFragments(fqName)
                val subPackages = packageFragmentProvider.getSubPackagesOf(fqName) { true }
                subPackages.forEach { getSubPackages(it) }
            }

            getSubPackages(FqName.ROOT)
            return result
        }
    }
}

/**
 * Reads KLIB, builds FIR on top of the deserialized metadata, builds Fir2LazyIr, collects signatures
 * from Fir2LazyIr nodes and compares them against the signatures previously collected by [AbstractCollectAndMemorizeIdSignatures].
 *
 * To be executed after `backendFacade` step.
 */
abstract class AbstractVerifyIdSignaturesByK2LazyIr(
    testServices: TestServices,
) : AbstractVerifyIdSignaturesByKlib(testServices) {
    protected abstract val fir2IrConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>

    final override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::FirModuleInfoProvider)) // Required for FirFrontendFacade. See processModule() below.

    final override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (isSignatureVerificationSkipped) return

        // Compile an empty module that depends on all modules where we want to extract lazy IR from.
        val emptyModule = createEmptyModuleThatDependsOnAllCompiledModules(testServices)
        val firOutput = FirFrontendFacade(testServices).analyze(emptyModule)
        val fir2IrOutput = fir2IrConverter(testServices).transform(emptyModule, firOutput)!!

        // Get top-level declaration FQ names.
        val signatureCollector = SignatureCollector.createForK2(PublicIdSignatureComputer(fir2IrOutput.irMangler))
        val irPluginContext = fir2IrOutput.irPluginContext

        allProducedKlibs().forEach { library ->
            library.loadTopLevelDeclarationSymbols(
                createClassSymbol = irPluginContext::referenceClass,
                createTypeAliasSymbol = irPluginContext::referenceTypeAlias,
                createFunctionSymbol = irPluginContext::referenceFunctions,
                createPropertySymbol = irPluginContext::referenceProperties
            ).forEach { symbol ->
                assertions.assertTrue(symbol.isBound) { "Unbound symbol: $symbol" }

                val declaration = symbol.owner
                assertions.assertTrue(declaration is AbstractFir2IrLazyDeclaration<*>) {
                    "Not a Fir2IrLazy declaration: ${declaration::class.java}, $declaration"
                }

                signatureCollector.collectFrom(declaration as IrDeclaration)
            }
        }

        assertions.assertEqualsToFile(
            expectedFile = testServices.signaturesDumpFile,
            actual = signatureCollector.toString()
        )
    }

    companion object {
        private fun createEmptyModuleThatDependsOnAllCompiledModules(testServices: TestServices): TestModule {
            val compiledModules = testServices.moduleStructure.modules

            // This is an approximation of all dependencies, which is hopefully enough.
            val approximatedDependencies =
                compiledModules.map { DependencyDescription(it.name, DependencyKind.KLib, DependencyRelation.RegularDependency) }

            val originalDirectives = testServices.moduleStructure.allDirectives
            // FIR_PARSER directive is required for FIR frontend. Need to add it unless it is already there.
            val patchedDirectives = if (FirDiagnosticsDirectives.FIR_PARSER !in originalDirectives)
                ComposedRegisteredDirectives(
                    originalDirectives,
                    RegisteredDirectivesBuilder().apply { FirDiagnosticsDirectives.FIR_PARSER with FirParser.LightTree }.build()
                )
            else originalDirectives
            // Need to avoid duplicated FIR_PARSER directives (but with the same value) that come from each test module:
            val uniqueDirectives = UniqueRegisteredDirectives(patchedDirectives)

            return compiledModules.first().copy(
                name = "--empty auxiliary module--",
                files = emptyList(),
                directives = uniqueDirectives,
                allDependencies = approximatedDependencies
            )
        }

        private inline fun MetadataLibrary.loadTopLevelDeclarationSymbols(
            createClassSymbol: (ClassId) -> IrClassSymbol?,
            createTypeAliasSymbol: (ClassId) -> IrTypeAliasSymbol?,
            createFunctionSymbol: (CallableId) -> Collection<IrSimpleFunctionSymbol>,
            createPropertySymbol: (CallableId) -> Collection<IrPropertySymbol>
        ): Collection<IrSymbol> {
            val symbols = hashSetOf<IrSymbol>()

            val fragmentNames: List<String> = parseModuleHeader(moduleHeaderData).packageFragmentNameList
            fragmentNames.forEach { fragmentName ->
                val packageFqName = FqName(fragmentName)

                packageMetadataParts(fragmentName).forEach { partName ->
                    val fragmentProto = parsePackageFragment(packageMetadata(fragmentName, partName))
                    val strings = NameResolverImpl(fragmentProto.strings, fragmentProto.qualifiedNames)

                    fragmentProto.class_List.forEach classProto@{ classProto ->
                        if (strings.isLocalClassName(classProto.fqName)) return@classProto

                        val classId = ClassId.fromString(strings.getQualifiedClassName(classProto.fqName))
                        if (classId.isNestedClass) return@classProto

                        symbols.addIfNotNull(createClassSymbol(classId))
                    }

                    fragmentProto.`package`.typeAliasList.forEach { typeAliasProto ->
                        val typeAliasId = ClassId(packageFqName, Name.identifier(strings.getString(typeAliasProto.name)))
                        symbols.addIfNotNull(createTypeAliasSymbol(typeAliasId))
                    }

                    fragmentProto.`package`.functionList.forEach { functionProto ->
                        val functionId = CallableId(packageFqName, Name.identifier(strings.getString(functionProto.name)))
                        symbols += createFunctionSymbol(functionId)
                    }

                    fragmentProto.`package`.propertyList.forEach { propertyProto ->
                        val propertyId = CallableId(packageFqName, Name.identifier(strings.getString(propertyProto.name)))
                        symbols += createPropertySymbol(propertyId)
                    }
                }
            }

            return symbols
        }
    }
}

private class SignatureCollector private constructor(private val computeSignature: (IrSymbol) -> IdSignature) {
    private val signatures: MutableSet<String> = hashSetOf()

    override fun toString() = signatures.sorted().joinToString(separator = "\n")

    fun collectFrom(declaration: IrDeclaration) {
        when (declaration) {
            is IrTypeAlias -> runIf(declaration.isVisibleDeclaration) {
                collectFrom(declaration.symbol)
            }

            is IrClass -> runIf(declaration.isVisibleDeclaration && !declaration.isExpect) {
                collectFrom(declaration.symbol)
                for (member in declaration.declarations) {
                    collectFrom(member)
                }
            }

            is IrFunction -> runIf(declaration.isVisibleDeclaration && !declaration.isExpect && !declaration.isFakeOverriddenFromAny()) {
                collectFrom(declaration.symbol)
            }

            is IrProperty -> runIf(declaration.isVisibleDeclaration && !declaration.isExpect) {
                collectFrom(declaration.symbol)
                declaration.getter?.let(::collectFrom)
                declaration.setter?.let(::collectFrom)
            }

            is IrEnumEntry -> collectFrom(declaration.symbol)
        }
    }

    private fun collectFrom(symbol: IrSymbol) {
        signatures.addIfNotNull(
            computeSignature(symbol).takeIf { it.isVisibleSignature }?.render(IdSignatureRenderer.LEGACY)
        )
    }

    companion object {
        fun createForK1(testServices: TestServices) = SignatureCollector { symbol ->
            // It is assumed that in K1 every symbol has already computed signature.
            symbol.signature ?: testServices.assertions.fail { "Symbol without signature: $symbol" }
        }

        fun createForK2(signatureComputer: PublicIdSignatureComputer) = SignatureCollector { symbol ->
            // The signature is normally missing in IR or lazy IR built on top of FIR.
            signatureComputer.computeSignature(symbol.owner as IrDeclaration)
        }

        private val IrDeclarationWithVisibility.isVisibleDeclaration: Boolean
            get() = visibility == DescriptorVisibilities.PUBLIC || visibility == DescriptorVisibilities.PROTECTED

        private val IdSignature.isVisibleSignature: Boolean
            get() = isPubliclyVisible && !isLocal
    }
}

private val TestServices.signaturesDumpFile: File
    get() = temporaryDirectoryManager.rootDir.resolve("signatures-dump.txt")

private val AnalysisHandler<*>.isSignatureVerificationSkipped: Boolean
    get() = testServices.moduleStructure.modules.any { SKIP_SIGNATURE_VERIFICATION in it.directives }

private val <A : ResultingArtifact<A>> AnalysisHandler<A>.producedArtifactsMap: Map<TestModule, A?>
    get() = testServices.moduleStructure.modules.associateWith { testServices.dependencyProvider.getArtifactSafe(it, artifactKind) }

private val AnalysisHandler<*>.arbitraryTestModule: TestModule
    get() = testServices.moduleStructure.modules.first()
