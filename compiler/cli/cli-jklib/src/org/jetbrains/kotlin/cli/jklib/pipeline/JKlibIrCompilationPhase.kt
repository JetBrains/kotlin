/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib.pipeline

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsPackageFragmentProvider
import org.jetbrains.kotlin.cli.jvm.compiler.AllJavaSourcesInProjectScope
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.toAbstractProjectFileSearchScope
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.deserialization.AdditionalClassPartsProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.java.di.createContainerForLazyResolveWithJava
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.jklib.JKlibDescriptorMangler
import org.jetbrains.kotlin.ir.backend.jklib.JKlibIrLinker
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.descriptors.IrDescriptorBasedFunctionFactory
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isJklibStdlib
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.load.java.lazy.ModuleClassResolver
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.VirtualFileBoundJavaClass
import org.jetbrains.kotlin.load.kotlin.JavaFlexibleTypeDeserializer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.multiplatform.OptionalAnnotationPackageFragmentProvider
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType

@OptIn(ObsoleteDescriptorBasedAPI::class)
object JKlibIrCompilationPhase :
    PipelinePhase<JKlibSerializationArtifact, JKlibIrCompilationArtifact>(
        name = "JKlibIrCompilationPhase",
        postActions = setOf(CheckCompilationErrors.CheckDiagnosticCollector),
    ) {

    override fun executePhase(input: JKlibSerializationArtifact): JKlibIrCompilationArtifact {
        val configuration = input.configuration
        val klib = File(input.outputKlibPath)

        val projectEnvironment = input.projectEnvironment

        val klibFiles = configuration.getList(JVMConfigurationKeys.KLIB_PATHS) + klib.absolutePath
        val projectContext = ProjectContext(projectEnvironment.project, "TopDownAnalyzer for JKlib")
        val storageManager = projectContext.storageManager
        val builtIns = JvmBuiltIns(projectContext.storageManager, JvmBuiltIns.Kind.FROM_DEPENDENCIES)
        val sortedDependencies = loadLibraries(klibFiles, configuration)

        val dependencyDescriptorsByKlib = createKlibDescriptors(sortedDependencies, configuration, storageManager, builtIns)

        val jarDepsModuleDescriptor = createJarDependenciesModuleDescriptor(
            projectEnvironment,
            projectContext,
            configuration,
            builtIns,
        )

        val descriptors = dependencyDescriptorsByKlib.values + jarDepsModuleDescriptor
        descriptors.forEach { if (it != jarDepsModuleDescriptor) it.setDependencies(descriptors) }

        val mainModule = dependencyDescriptorsByKlib.getValue(sortedDependencies.single { it.libraryFile == klib })

        val trace = BindingTraceContext(projectContext.project)
        val mangler = JKlibDescriptorMangler(
            MainFunctionDetector(trace.bindingContext, configuration.languageVersionSettings)
        )
        val symbolTable = SymbolTable(IdSignatureDescriptor(mangler), IrFactoryImpl)
        val typeTranslator = TypeTranslatorImpl(symbolTable, configuration.languageVersionSettings, mainModule)
        val irBuiltIns = IrBuiltInsOverDescriptors(mainModule.builtIns, typeTranslator, symbolTable)

        val stubGenerator = DeclarationStubGeneratorImpl(
            mainModule,
            symbolTable,
            irBuiltIns,
            DescriptorByIdSignatureFinderImpl(mainModule, mangler),
            JvmGeneratorExtensionsImpl(configuration),
        ).apply { unboundSymbolGeneration = true }
        val linker = JKlibIrLinker(
            module = mainModule,
            configuration = configuration,
            irBuiltIns = irBuiltIns,
            symbolTable = symbolTable,
            stubGenerator = stubGenerator,
            mangler = mangler,
        )

        val pluginContext = IrPluginContextImpl(
            mainModule,
            configuration.languageVersionSettings,
            symbolTable,
            irBuiltIns,
            linker = linker,
            messageCollector = @OptIn(MessageCollectorAccess::class) /* deprecated in IrPluginContext */ configuration.messageCollector,
        )

        // Deserialize modules
        // We explicitly use the DeserializationStrategy.ALL to deserialize the whole world,
        // so that we don't rely on linker side effects for proper deserialization.
        linker.deserializeIrModuleHeader(
            jarDepsModuleDescriptor,
            null,
            { DeserializationStrategy.ALL },
            jarDepsModuleDescriptor.name.asString(),
        )

        lateinit var mainModuleFragment: IrModuleFragment
        for ((dep, descriptor) in dependencyDescriptorsByKlib) {
            when {
                descriptor == mainModule -> {
                    mainModuleFragment = linker.deserializeIrModuleHeader(descriptor, dep, { DeserializationStrategy.ALL })
                }
                else -> linker.deserializeIrModuleHeader(descriptor, dep, { DeserializationStrategy.ALL })
            }
        }

        irBuiltIns.functionFactory = IrDescriptorBasedFunctionFactory(
            irBuiltIns,
            symbolTable,
            typeTranslator,
            getPackageFragment = null,
            referenceFunctionsWhenKFunctionAreReferenced = true
        )

        linker.init(null)
        ExternalDependenciesGenerator(symbolTable, listOf(linker)).generateUnboundSymbolsAsDependencies()
        linker.postProcess(inOrAfterLinkageStep = true)

        // TODO(KT-86160): remove this when we have a proper solution for this issue.
        symbolTable
            .referenceClass(IdSignature.CommonSignature("kotlin", "Int", null, 0, null))
            .owner
            .declarations

        linker.checkNoUnboundSymbols(symbolTable, "Found unbound symbol")

        return JKlibIrCompilationArtifact(
            pluginContext,
            mainModuleFragment,
            configuration,
        )
    }

    private fun createJarDependenciesModuleDescriptor(
        projectEnvironment: VfsBasedProjectEnvironment,
        projectContext: ProjectContext,
        configuration: CompilerConfiguration,
        builtIns: JvmBuiltIns,
    ): ModuleDescriptorImpl {
        val languageVersionSettings = configuration.languageVersionSettings
        val platform = JvmPlatforms.defaultJvmPlatform
        val dependenciesContext = ContextForNewModule(
            projectContext,
            Name.special("<dependencies of ${configuration.getNotNull(MODULE_NAME)}>"),
            builtIns,
            platform,
        )

        // Scope for the dependency module contains everything except files present in the scope for the
        // source module
        val scope = AllJavaSourcesInProjectScope(projectContext.project)
        val dependencyScope = GlobalSearchScope.notScope(scope)


        val moduleClassResolver = SourceOrBinaryModuleClassResolver(scope)
        val lookupTracker = LookupTracker.DO_NOTHING
        val expectActualTracker = ExpectActualTracker.DoNothing
        val inlineConstTracker = InlineConstTracker.DoNothing
        val enumWhenTracker = EnumWhenTracker.DoNothing

        val configureJavaClassFinder = null
        val implicitsResolutionFilter = null
        val packagePartProvider = projectEnvironment.getPackagePartProvider(dependencyScope.toAbstractProjectFileSearchScope())
        val trace = NoScopeRecordCliBindingTrace(projectContext.project)
        val dependenciesContainer = createContainerForLazyResolveWithJava(
            platform,
            dependenciesContext,
            trace,
            DeclarationProviderFactory.EMPTY,
            dependencyScope,
            moduleClassResolver,
            CompilerEnvironment,
            lookupTracker,
            expectActualTracker,
            inlineConstTracker,
            enumWhenTracker,
            packagePartProvider,
            languageVersionSettings,
            useBuiltInsProvider = true,
            configureJavaClassFinder = configureJavaClassFinder,
            implicitsResolutionFilter = implicitsResolutionFilter,
        )
        moduleClassResolver.compiledCodeResolver = dependenciesContainer.get()

        dependenciesContext.setDependencies(
            listOf(dependenciesContext.module, builtIns.builtInsModule)
        )
        dependenciesContext.initializeModuleContents(
            CompositePackageFragmentProvider(
                listOf(
                    moduleClassResolver.compiledCodeResolver.packageFragmentProvider,
                    dependenciesContainer.get<JvmBuiltInsPackageFragmentProvider>(),
                    dependenciesContainer.get<OptionalAnnotationPackageFragmentProvider>(),
                ),
                "CompositeProvider@TopDownAnalyzerForJvm for dependencies ${dependenciesContext.module}",
            )
        )
        return dependenciesContext.module
    }

    private fun createKlibDescriptors(
        sortedDependencies: List<KotlinLibrary>,
        configuration: CompilerConfiguration,
        storageManager: StorageManager,
        builtIns: JvmBuiltIns,
    ): Map<KotlinLibrary, ModuleDescriptorImpl> {
        val klibFactories = KlibMetadataFactories(
            { builtIns },
            JavaFlexibleTypeDeserializer,
            // We need to wire the JvmBuiltInsCustomizer instance to the KlibMetadataFactories. This allows resolving APIs that are not part
            // of the original builtins classes but are present in the mapped Java classes.
            // We cannot directly pass builtIns.customizer because it's a lazy property. It can only be accessed after builtIns has been
            // fully initialized. By the time the deserializer queries this provider, `builtIns` will have been fully initialized.
            additionalClassPartsProvider = object : AdditionalClassPartsProvider {
                private val delegate by lazy { builtIns.customizer }

                override fun getSupertypes(classDescriptor: ClassDescriptor): Collection<KotlinType> =
                    delegate.getSupertypes(classDescriptor)

                override fun getFunctions(name: Name, classDescriptor: ClassDescriptor): Collection<SimpleFunctionDescriptor> =
                    delegate.getFunctions(name, classDescriptor)

                override fun getConstructors(classDescriptor: ClassDescriptor): Collection<ClassConstructorDescriptor> =
                    delegate.getConstructors(classDescriptor)

                override fun getFunctionsNames(classDescriptor: ClassDescriptor): Collection<Name> =
                    delegate.getFunctionsNames(classDescriptor)
            }
        )

        val dependencyDescriptorsByKlib = sortedDependencies.associateWith { klib ->
            val descriptor = klibFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
                klib,
                configuration.languageVersionSettings,
                storageManager,
                if (klib.isJklibStdlib) null else builtIns,
                lookupTracker = LookupTracker.DO_NOTHING,
            )

            if (klib.isJklibStdlib) {
                builtIns.initialize(descriptor, true)
            }

            descriptor
        }

        return dependencyDescriptorsByKlib
    }

    private fun loadLibraries(klibFiles: List<String>, configuration: CompilerConfiguration): List<KotlinLibrary> {
        val loadingResult = KlibLoader { libraryPaths(klibFiles) }.load()
        loadingResult.reportLoadingProblemsIfAny(configuration, allAsErrors = true)
        return loadingResult.librariesStdlibFirst
    }
}

private class SourceOrBinaryModuleClassResolver(private val sourceScope: GlobalSearchScope) : ModuleClassResolver {
    lateinit var compiledCodeResolver: JavaDescriptorResolver
    lateinit var sourceCodeResolver: JavaDescriptorResolver

    override fun resolveClass(javaClass: JavaClass): ClassDescriptor? {
        val resolver = if (javaClass is VirtualFileBoundJavaClass && javaClass.isFromSourceCodeInScope(sourceScope)) sourceCodeResolver
        else compiledCodeResolver
        return resolver.resolveClass(javaClass)
    }
}
