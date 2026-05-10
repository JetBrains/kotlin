/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.impl.base.util.withKaModuleEntry
import org.jetbrains.kotlin.analysis.api.platform.KaCachedService
import org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.platform.declarations.*
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaDanglingFileModuleImpl
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScopeProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinAnchorModuleProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.platform.utils.mergeInto
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirGlobalResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirLazyDeclarationResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirIdeRegisteredPluginAnnotations
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirLibrarySessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLNameConflictsTracker
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.cache.LLFirSessionCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.cache.configure
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.components.LLPlatformSessionComponentRegistration
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.configuration.LLPlatformSessionConfiguration
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.combined.LLCombinedJavaSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.combined.LLCombinedKotlinSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.combined.LLCombinedPackageDelegationSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.combined.LLCombinedSyntheticFunctionSymbolProvider
import org.jetbrains.kotlin.assignment.plugin.AssignmentCommandLineProcessor
import org.jetbrains.kotlin.assignment.plugin.AssignmentConfigurationKeys
import org.jetbrains.kotlin.assignment.plugin.k2.FirAssignmentPluginExtensionRegistrar
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.plugins.processCompilerPluginsOptions
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.KtRegisteredDiagnosticFactoriesStorage
import org.jetbrains.kotlin.fir.FirNameConflictsTracker
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.deserialization.FirKDocDeserializer
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirExtensionSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.syntheticFunctionInterfacesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.resolve.transformers.FirDummyCompilerLazyDeclarationResolver
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.scripting.compiler.plugin.FirScriptingSamWithReceiverExtensionRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.makeScriptCompilerArguments
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import org.jetbrains.kotlin.utils.exceptions.withVirtualFileEntry

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
internal class LLFirSessionFactory(
    private val project: Project,
    targetPlatform: TargetPlatform,
) {
    private val platformConfiguration = LLPlatformSessionConfiguration.forPlatform(targetPlatform, project)

    private val platformComponentRegistrations = LLPlatformSessionComponentRegistration.forPlatform(targetPlatform)

    @KaCachedService
    private val globalResolveComponents: LLFirGlobalResolveComponents by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLFirGlobalResolveComponents.getInstance(project)
    }

    @KaCachedService
    private val resolutionScopeProvider: KaResolutionScopeProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KaResolutionScopeProvider.getInstance(project)
    }

    @KaCachedService
    private val platformSettings: KotlinPlatformSettings by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KotlinPlatformSettings.getInstance(project)
    }

    fun createSourcesSession(module: KaSourceModule): LLFirSourcesSession {
        val platform = module.targetPlatform
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform)
        val languageVersionSettings = wrapLanguageVersionSettings(module.languageVersionSettings)

        val scopeProvider = platformConfiguration.createSourceScopeProvider()
        val components = LLFirModuleResolveComponents(module, globalResolveComponents, scopeProvider)

        val session = LLFirSourcesSession(module, components, builtinsSession.builtinTypes) {
            computeDependencySessions(module)
        }

        components.session = session

        val moduleData = createModuleData(session)
        val resolutionScope = resolutionScopeProvider.getResolutionScope(module)

        return session.apply {
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            registerAllCommonComponents(languageVersionSettings, resolutionScope)
            registerSourceLikeComponents()

            val firProvider = LLFirProvider(
                this,
                components,
                /* Source modules can contain `kotlin` package only if `-Xallow-kotlin-package` is specified, this is handled in LLFirProvider */
                canContainKotlinPackage = false,
            ) { scope ->
                project.createDeclarationProvider(scope, module)
            }

            register(FirProvider::class, firProvider)
            register(FirLazyDeclarationResolver::class, LLFirLazyDeclarationResolver())

            registerCompilerPluginServices(project, resolutionScope)
            registerCompilerPluginExtensions(project, module)
            registerCommonComponentsAfterExtensionsAreConfigured()

            val dependencyProvider = LLDependenciesSymbolProvider(this) {
                buildList {
                    addMerged(session, computeDependencySymbolProviders(session.dependencies))
                    add(builtinsSession.symbolProvider)
                }
            }

            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)

            LLFirSessionConfigurator.configure(this)

            val syntheticFunctionInterfaceProvider =
                FirExtensionSyntheticFunctionInterfaceProvider.createIfNeeded(this, moduleData, scopeProvider)

            val switchableExtensionDeclarationsSymbolProvider =
                LLFirSwitchableExtensionDeclarationsSymbolProvider
                    .createIfNeeded(this)
                    ?.also { register(FirSwitchableExtensionDeclarationsSymbolProvider::class, it) }

            register(
                FirBuiltinSyntheticFunctionInterfaceProvider::class,
                builtinsSession.syntheticFunctionInterfacesSymbolProvider
            )

            val platformSpecificSymbolProviders = platformConfiguration.createPlatformSpecificSymbolProviders(this, module.contentScope)
            register(
                FirSymbolProvider::class,
                LLModuleWithDependenciesSymbolProvider(
                    this,
                    providers = buildList {
                        add(firProvider.symbolProvider)
                        addIfNotNull(switchableExtensionDeclarationsSymbolProvider)
                        addAll(platformSpecificSymbolProviders)
                        addIfNotNull(syntheticFunctionInterfaceProvider)
                    },
                    dependencyProvider,
                )
            )

            session.registerPlatformSpecificComponents(platformSpecificSymbolProviders) { registration ->
                registration.registerSourceComponents(this)
            }
        }
    }

    fun createResolvableLibrarySession(module: KaModule): LLFirLibraryOrLibrarySourceResolvableModuleSession {
        val binaryModule = when (module) {
            is KaLibraryModule, is KaBuiltinsModule -> module
            is KaLibrarySourceModule -> module.binaryLibrary
            else -> errorWithAttachment("Unexpected module ${module::class.simpleName}") {
                withKaModuleEntry("module", module)
            }
        }

        val platform = module.targetPlatform
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform)
        val languageVersionSettings = KotlinProjectStructureProvider.getInstance(project).libraryLanguageVersionSettings

        val scopeProvider = FirKotlinScopeProvider()
        val components = LLFirModuleResolveComponents(module, globalResolveComponents, scopeProvider)

        val session = LLFirLibraryOrLibrarySourceResolvableModuleSession(module, components, builtinsSession.builtinTypes)
        components.session = session

        val moduleData = createModuleData(session)
        val binaryContentScope = binaryModule.contentScope

        return session.apply {
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            registerAllCommonComponents(languageVersionSettings, binaryContentScope)
            registerCommonComponentsAfterExtensionsAreConfigured()

            val firProvider = LLFirProvider(
                this,
                components,
                canContainKotlinPackage = true,
            ) { scope ->
                project.createDeclarationProvider(scope, module)
            }

            register(FirProvider::class, firProvider)

            register(FirLazyDeclarationResolver::class, LLFirLazyDeclarationResolver())

            // We need FirRegisteredPluginAnnotations during extensions' registration process
            val annotationsResolver = project.createAnnotationResolver(binaryContentScope)
            register(FirRegisteredPluginAnnotations::class, LLFirIdeRegisteredPluginAnnotations(this, annotationsResolver))
            register(FirPredicateBasedProvider::class, FirEmptyPredicateBasedProvider)

            val dependencyProvider = LLDependenciesSymbolProvider(this) {
                buildList {
                    if (module !is KaBuiltinsModule) {
                        add(builtinsSession.symbolProvider)
                    }

                    // The library (source) module will usually have a `KaLibraryFallbackDependenciesModule`, which will be added here, but
                    // this also works when the library (source) module has precise dependencies.
                    addMerged(session, computeDependencySymbolProviders(binaryModule))

                    if (binaryModule is KaLibraryModule) {
                        KotlinAnchorModuleProvider.getInstance(project)?.getAnchorModule(binaryModule)?.let { anchorModule ->
                            val anchorModuleSession = LLFirSessionCache.getInstance(project).getSession(anchorModule)
                            val anchorModuleSymbolProvider =
                                anchorModuleSession.symbolProvider as LLModuleWithDependenciesSymbolProvider

                            addAll(anchorModuleSymbolProvider.providers)
                            addAll(anchorModuleSymbolProvider.dependencyProvider.providers)
                        }
                    }
                }
            }

            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)

            val platformSpecificSymbolProviders = platformConfiguration.createPlatformSpecificSymbolProviders(this, binaryContentScope)
            register(
                FirSymbolProvider::class,
                LLModuleWithDependenciesSymbolProvider(
                    this,
                    providers = buildList {
                        add(firProvider.symbolProvider)
                        addAll(platformSpecificSymbolProviders)
                    },
                    dependencyProvider,
                )
            )

            session.registerPlatformSpecificComponents(platformSpecificSymbolProviders) { registration ->
                registration.registerResolvableLibraryComponents(this)
            }

            LLFirSessionConfigurator.configure(this)
        }
    }

    /**
     * Creates a binary [LLFirLibrarySession] for a [KaLibraryModule] or [KaLibraryFallbackDependenciesModule].
     *
     * Both regular libraries and library fallback dependencies can be treated from the same point of view of a binary session. Hence, it
     * doesn't make practical sense to have separate session creation machinery for [KaLibraryFallbackDependenciesModule].
     */
    fun createBinaryLibrarySession(module: KaModule): LLFirLibrarySession {
        require(module is KaLibraryModule || module is KaLibraryFallbackDependenciesModule) {
            "A binary library session can only be created for a `${KaLibraryModule::class.simpleName}` or a " +
                    "`${KaLibraryFallbackDependenciesModule::class.simpleName}`. Instead got: `${module::class.simpleName}`."
        }

        val platform = module.targetPlatform
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform)

        val session = LLFirLibrarySession(module, builtinsSession.builtinTypes)

        val moduleData = createModuleData(session)
        val contentScope = module.contentScope

        return session.apply {
            val languageVersionSettings = KotlinProjectStructureProvider.getInstance(project).libraryLanguageVersionSettings
            registerModuleData(moduleData)
            registerIdeComponents(project, languageVersionSettings, contentScope)
            register(FirLazyDeclarationResolver::class, FirDummyCompilerLazyDeclarationResolver)
            registerCommonComponents(languageVersionSettings, isMetadataCompilation = session.isMetadataSession)
            registerCommonComponentsAfterExtensionsAreConfigured()
            registerKdocDeserializer()

            val kotlinScopeProvider = when {
                platform.isJvm() -> FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
                else -> FirKotlinScopeProvider()
            }

            register(FirKotlinScopeProvider::class, kotlinScopeProvider)

            val symbolProvider = LLModuleWithDependenciesSymbolProvider(
                this,
                providers = platformConfiguration.createBinaryLibrarySymbolProviders(this, contentScope),
                LLDependenciesSymbolProvider(this) {
                    // A binary library session should not have any dependencies (apart from fallback builtins), as library module
                    // dependencies only apply to *resolvable* sessions, including fallback dependencies.
                    listOf(builtinsSession.symbolProvider)
                },
            )

            register(FirProvider::class, LLFirLibrarySessionProvider(symbolProvider))
            register(FirSymbolProvider::class, symbolProvider)

            session.registerPlatformSpecificComponents(platformSpecificSymbolProviders = emptyList()) { registration ->
                registration.registerBinaryLibraryComponents(this)
            }

            LLFirSessionConfigurator.configure(this)
        }
    }

    private fun LLFirSession.registerKdocDeserializer() {
        if (ktModule.targetPlatform.isJvm()) {
            // Only KLib-based platforms are supported
            return
        }

        if (platformSettings.deserializedDeclarationsOrigin == KotlinDeserializedDeclarationsOrigin.STUBS) {
            // KDoc is always deserialized in stubs. No need to put it also to the FIR
            return
        }

        register(FirKDocDeserializer::class, KlibBasedKDocDeserializer)
    }

    fun createScriptSession(module: KaScriptModule): LLFirScriptSession {
        val platform = module.targetPlatform
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform)
        val languageVersionSettings = wrapLanguageVersionSettings(module.languageVersionSettings)
        val scopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)

        val components = LLFirModuleResolveComponents(module, globalResolveComponents, scopeProvider)

        val session = LLFirScriptSession(module, components, builtinsSession.builtinTypes)
        components.session = session

        val moduleData = createModuleData(session)
        val resolutionScope = resolutionScopeProvider.getResolutionScope(module)

        return session.apply {
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            registerAllCommonComponents(languageVersionSettings, resolutionScope)
            registerSourceLikeComponents()

            registerCommonComponentsAfterExtensionsAreConfigured()
            registerJavaComponents(JavaModuleResolver.getInstance(project))

            val provider = LLFirProvider(
                this,
                components,
                canContainKotlinPackage = true,
            ) { scope ->
                createScopedDeclarationProviderForFiles(scope, listOf(module.file))
            }

            register(FirProvider::class, provider)
            register(FirLazyDeclarationResolver::class, LLFirLazyDeclarationResolver())

            val dependencyProvider = LLDependenciesSymbolProvider(this) {
                buildList {
                    addMerged(session, computeDependencySymbolProviders(module))
                    add(builtinsSession.symbolProvider)
                }
            }

            val javaSymbolProvider = LLFirJavaSymbolProvider(this, module.contentScope)
            register(JavaSymbolProvider::class, javaSymbolProvider)

            register(
                FirSymbolProvider::class,
                LLModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOfNotNull(
                        javaSymbolProvider,
                        provider.symbolProvider,
                    ),
                    dependencyProvider,
                )
            )

            register(FirPredicateBasedProvider::class, FirEmptyPredicateBasedProvider)
            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)
            register(FirRegisteredPluginAnnotations::class, FirRegisteredPluginAnnotationsImpl(this))
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))

            FirSessionConfigurator(session).apply {
                registerCompilerPluginExtensions(project, module)
                registerScriptExtensions(module.file)
            }.configure()

            LLFirSessionConfigurator.configure(this)
        }
    }

    @OptIn(ExperimentalCompilerApi::class)
    private fun FirSessionConfigurator.registerScriptExtensions(file: KtFile) {
        val scriptDefinition = file.findScriptDefinition() ?: errorWithAttachment("Cannot load script definition") {
            withVirtualFileEntry("file", file.virtualFile)
        }

        val hostConfiguration = scriptDefinition.hostConfiguration
        registerExtensions(FirReplCompilerExtensionIdeRegistrar(hostConfiguration).configure())

        val compilerArguments = makeScriptCompilerArguments(scriptDefinition.compilerOptions.toList())
        val commandLineProcessors = listOf(AssignmentCommandLineProcessor())
        val compilerConfiguration = CompilerConfiguration.create()
        processCompilerPluginsOptions(
            compilerConfiguration, compilerArguments.pluginOptions.asIterable(), commandLineProcessors
        )

        val extensionRegistrar = FirScriptingCompilerExtensionIdeRegistrar(
            project,
            hostConfiguration,
            scriptDefinitionSources = emptyList(),
            scriptDefinitions = listOf(scriptDefinition)
        )

        registerExtensions(extensionRegistrar.configure())
        registerExtensions(FirScriptingSamWithReceiverExtensionRegistrar().configure())

        compilerConfiguration.getList(AssignmentConfigurationKeys.ASSIGNMENT_ANNOTATION).takeIf { it.isNotEmpty() }?.let {
            registerExtensions(FirAssignmentPluginExtensionRegistrar(it).configure())
        }
    }

    fun createNotUnderContentRootResolvableSession(module: KaNotUnderContentRootModule): LLFirNotUnderContentRootResolvableModuleSession {
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(JvmPlatforms.unspecifiedJvmPlatform)
        val languageVersionSettings = KotlinProjectStructureProvider.getInstance(project).globalLanguageVersionSettings
        val scopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
        val components = LLFirModuleResolveComponents(module, globalResolveComponents, scopeProvider)

        val session = LLFirNotUnderContentRootResolvableModuleSession(module, components, builtinsSession.builtinTypes)
        components.session = session

        val moduleData = createModuleData(session)
        val resolutionScope = resolutionScopeProvider.getResolutionScope(module)

        return session.apply {
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            registerAllCommonComponents(languageVersionSettings, resolutionScope)
            registerSourceLikeComponents()

            registerCommonComponentsAfterExtensionsAreConfigured()
            registerJavaComponents(JavaModuleResolver.getInstance(project))


            val ktFile = module.file as? KtFile

            val provider = LLFirProvider(
                this,
                components,
                canContainKotlinPackage = true,
            ) { scope ->
                createScopedDeclarationProviderForFiles(scope, listOfNotNull(ktFile))
            }

            register(FirProvider::class, provider)
            register(FirLazyDeclarationResolver::class, LLFirLazyDeclarationResolver())

            val dependencyProvider = LLDependenciesSymbolProvider(this) {
                buildList {
                    addMerged(session, computeDependencySymbolProviders(module))
                    add(builtinsSession.symbolProvider)
                }
            }

            register(
                FirSymbolProvider::class,
                LLModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOf(
                        provider.symbolProvider,
                    ),
                    dependencyProvider,
                )
            )

            register(FirPredicateBasedProvider::class, FirEmptyPredicateBasedProvider)
            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
            register(FirRegisteredPluginAnnotations::class, FirRegisteredPluginAnnotations.Empty)

            LLFirSessionConfigurator.configure(this)
        }
    }

    fun createDanglingFileSession(module: KaDanglingFileModule, contextSession: LLFirSession): LLFirSession {
        val platform = module.targetPlatform

        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform)
        val languageVersionSettings = wrapLanguageVersionSettings(contextSession.languageVersionSettings)
        val scopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)

        val components = LLFirModuleResolveComponents(module, globalResolveComponents, scopeProvider)

        val session = LLFirDanglingFileSession(module, components, builtinsSession.builtinTypes)
        components.session = session

        val moduleData = createModuleData(session)
        val resolutionScope = resolutionScopeProvider.getResolutionScope(module)

        return session.apply {
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            registerAllCommonComponents(languageVersionSettings, resolutionScope)
            registerSourceLikeComponents()

            val firProvider = LLFirProvider(
                this,
                components,
                canContainKotlinPackage = true,
                disregardSelfDeclarations = module.resolutionMode == KaDanglingFileResolutionMode.IGNORE_SELF,
                declarationProviderFactory = { scope -> createScopedDeclarationProviderForFiles(scope, module.files) }
            )

            register(FirProvider::class, firProvider)
            register(FirLazyDeclarationResolver::class, LLFirLazyDeclarationResolver())

            register(FirRegisteredPluginAnnotations::class, FirRegisteredPluginAnnotationsImpl(session))
            register(FirPredicateBasedProvider::class, FirEmptyPredicateBasedProvider)

            val contextModule = module.contextModule
            when (contextModule) {
                is KaSourceModule -> {
                    registerCompilerPluginServices(project, resolutionScope)
                    registerCompilerPluginExtensions(project, contextModule)
                }
                is KaScriptModule -> {
                    FirSessionConfigurator(session).apply {
                        registerScriptExtensions(contextModule.file)
                        registerCompilerPluginExtensions(project, contextModule)
                    }.configure()
                }
            }

            registerCommonComponentsAfterExtensionsAreConfigured()

            val dependencyProvider = LLDependenciesSymbolProvider(this) {
                buildList {
                    // The default implementation must have no extra dependencies (so we can delegate to the context module dependencies).
                    // For other implementations, we need to at least perform the check.
                    if (module !is KaDanglingFileModuleImpl) {
                        val allDependencies = computeAggregatedModuleDependencies(module)
                        val contextDependencies = computeAggregatedModuleDependencies(contextModule)

                        val hasAllContextDependencies = contextDependencies.all { it in allDependencies }
                        if (hasAllContextDependencies) {
                            // Exclude dependencies of the context module as they are submitted below
                            val ownDependencies = allDependencies - contextDependencies
                            if (ownDependencies.isNotEmpty()) {
                                val dependencySessions = computeDependencySessionsFromDependencyModules(ownDependencies, module)
                                addMerged(session, computeDependencySymbolProviders(dependencySessions))
                            }
                            // Share symbol providers (and their caches) with the context session
                            addMerged(session, computeDependencySymbolProviders(listOf(contextSession)))
                        } else {
                            // Dependencies are original, so we need a separate set of providers
                            val dependencySessions = computeDependencySessionsFromDependencyModules(allDependencies, module)
                            addMerged(session, computeDependencySymbolProviders(dependencySessions))
                        }
                    } else {
                        addMerged(session, computeDependencySymbolProviders(listOf(contextSession)))
                    }

                    when (contextSession.ktModule) {
                        is KaLibraryModule, is KaLibrarySourceModule -> {
                            // Wrap library dependencies into a single classpath-filtering provider
                            // Also see 'LLDanglingFileDependenciesSymbolProvider.filterSymbols()'
                            add(LLDanglingFileDependenciesSymbolProvider(contextSession.dependenciesSymbolProvider))
                        }
                        else -> add(contextSession.dependenciesSymbolProvider)
                    }

                    add(builtinsSession.symbolProvider)
                }
            }

            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)

            LLFirSessionConfigurator.configure(this)

            val syntheticFunctionInterfaceProvider = FirExtensionSyntheticFunctionInterfaceProvider
                .createIfNeeded(this, moduleData, scopeProvider)

            val switchableExtensionDeclarationsSymbolProvider = LLFirSwitchableExtensionDeclarationsSymbolProvider
                .createIfNeeded(this)
                ?.also { register(FirSwitchableExtensionDeclarationsSymbolProvider::class, it) }

            val platformSpecificSymbolProviders =
                platformConfiguration.createPlatformSpecificSymbolProvidersForDanglingFileSession(this, contextSession)

            register(
                FirSymbolProvider::class,
                LLModuleWithDependenciesSymbolProvider(
                    this,
                    providers = buildList {
                        add(firProvider.symbolProvider)
                        addIfNotNull(switchableExtensionDeclarationsSymbolProvider)
                        addAll(platformSpecificSymbolProviders)
                        addIfNotNull(syntheticFunctionInterfaceProvider)
                    },
                    dependencyProvider,
                )
            )

            session.registerPlatformSpecificComponents(platformSpecificSymbolProviders) { registration ->
                registration.registerDanglingFileComponents(this)
            }
        }
    }

    private fun wrapLanguageVersionSettings(original: LanguageVersionSettings): LanguageVersionSettings {
        return object : LanguageVersionSettings by original {

            override fun <T> getFlag(flag: AnalysisFlag<T>): T {
                @Suppress("UNCHECKED_CAST")
                if (flag == JvmAnalysisFlags.suppressMissingBuiltinsError) return true as T
                return original.getFlag(flag)
            }

            override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State {
                return when (feature) {
                    LanguageFeature.EnableDfaWarningsInK2 -> LanguageFeature.State.ENABLED
                    else -> original.getFeatureSupport(feature)
                }
            }

            override fun supportsFeature(feature: LanguageFeature): Boolean {
                return when (getFeatureSupport(feature)) {
                    LanguageFeature.State.ENABLED -> true
                    else -> false
                }
            }
        }
    }

    private fun computeDependencySessions(module: KaModule): List<LLFirSession> {
        val dependencyModules = computeAggregatedModuleDependencies(module)
        return computeDependencySessionsFromDependencyModules(dependencyModules, module)
    }

    private fun computeAggregatedModuleDependencies(module: KaModule): Set<KaModule> {
        // Please update KmpModuleSorterTest#buildDependenciesToTest if the logic of collecting dependencies changes
        return buildSet {
            addAll(module.directRegularDependencies)
            addAll(module.directFriendDependencies)

            // The dependency provider needs to have access to all direct and indirect `dependsOn` dependencies, as `dependsOn`
            // dependencies are transitive.
            addAll(module.transitiveDependsOnDependencies)
        }
    }

    private fun computeDependencySessionsFromDependencyModules(dependencyModules: Set<KaModule>, module: KaModule): List<LLFirSession> {
        val sessionCache = LLFirSessionCache.getInstance(project)

        fun getOrCreateSessionForDependency(dependency: KaModule): LLFirSession? = when (dependency) {
            is KaBuiltinsModule -> null // Built-ins are already added

            is KaLibraryModule, is KaLibraryFallbackDependenciesModule -> sessionCache.getDependencySession(dependency)

            is KaSourceModule -> sessionCache.getDependencySession(dependency)

            is KaScriptModule -> sessionCache.getDependencySession(dependency)

            is KaDanglingFileModule -> {
                requireWithAttachment(dependency.isStable, message = { "Unstable dangling modules cannot be used as a dependency" }) {
                    withKaModuleEntry("module", module)
                    withKaModuleEntry("dependency", dependency)
                    dependency.files.forEachIndexed { index, file -> withPsiEntry("dependencyFile$index", file) }
                }
                sessionCache.getDependencySession(dependency)
            }

            else -> {
                errorWithAttachment("Module ${module::class} cannot depend on ${dependency::class}") {
                    withKaModuleEntry("module", module)
                    withKaModuleEntry("dependency", dependency)
                }
            }
        }

        val orderedDependencyModules = KmpModuleSorter.order(dependencyModules.toList())

        return orderedDependencyModules.mapNotNull(::getOrCreateSessionForDependency)
    }

    private fun computeDependencySymbolProviders(module: KaModule): List<FirSymbolProvider> =
        computeDependencySymbolProviders(computeDependencySessions(module))

    private fun computeDependencySymbolProviders(dependencySessions: List<LLFirSession>): List<FirSymbolProvider> =
        buildList {
            dependencySessions.forEach { session ->
                when (val dependencyProvider = session.symbolProvider) {
                    is LLModuleWithDependenciesSymbolProvider -> dependencyProvider.providers.forEach { it.flattenTo(this) }
                    else -> dependencyProvider.flattenTo(this)
                }
            }
        }

    private fun FirSymbolProvider.flattenTo(destination: MutableList<FirSymbolProvider>) {
        when (this) {
            is FirCompositeSymbolProvider -> providers.forEach { it.flattenTo(destination) }
            else -> destination.add(this)
        }
    }

    private fun createModuleData(session: LLFirSession): LLFirModuleData {
        return LLFirModuleData(session)
    }

    private fun LLFirSession.registerAllCommonComponents(
        languageVersionSettings: LanguageVersionSettings,
        annotationSearchScope: GlobalSearchScope,
    ) {
        registerIdeComponents(project, languageVersionSettings, annotationSearchScope)
        registerCommonComponents(languageVersionSettings, isMetadataCompilation = isMetadataSession)
        registerResolveComponents(KtRegisteredDiagnosticFactoriesStorage())
    }

    private fun LLFirSession.registerSourceLikeComponents() {
        register(FirNameConflictsTracker::class, LLNameConflictsTracker(this))
    }

    private inline fun LLFirSession.registerPlatformSpecificComponents(
        platformSpecificSymbolProviders: List<FirSymbolProvider>,
        registerAdditionalComponents: (LLPlatformSessionComponentRegistration) -> Unit,
    ) {
        platformComponentRegistrations.forEach { registration ->
            registration.registerComponents(this, platformSpecificSymbolProviders)
            registerAdditionalComponents(registration)
        }
    }

    /**
     * Merges dependency symbol providers of the same kind, and adds the result to the receiver [MutableList].
     * See [mergeDependencySymbolProvidersInto] for more information on symbol provider merging.
     */
    private fun MutableList<FirSymbolProvider>.addMerged(session: LLFirSession, dependencies: List<FirSymbolProvider>) {
        dependencies.mergeDependencySymbolProvidersInto(session, this)
    }

    /**
     * Merges dependency symbol providers of the same kind if possible. The merged symbol provider usually delegates to its subordinate
     * symbol providers to preserve session semantics, but it will have some form of advantage over individual symbol providers (such as
     * querying an index once instead of N times).
     *
     * [session] should be the session of the dependent module. Because all symbol providers are tied to a session, we need a session to
     * create a combined symbol provider.
     */
    private fun List<FirSymbolProvider>.mergeDependencySymbolProvidersInto(
        session: LLFirSession,
        destination: MutableList<FirSymbolProvider>,
    ) {
        mergeInto(destination) {
            merge<LLKotlinSourceSymbolProvider> { LLCombinedKotlinSymbolProvider.merge(session, project, it) }

            // We place the combined Kotlin library symbol provider before the combined Java symbol provider because the former is generally
            // faster due to package and name set checks. The placement leads to fewer requests for class-like symbols arriving at the Java
            // symbol provider, since the Kotlin library symbol provider answers a good number of them.
            merge<LLKotlinStubBasedLibrarySymbolProvider> { LLCombinedPackageDelegationSymbolProvider.merge(session, it) }

            merge<LLFirJavaSymbolProvider> { LLCombinedJavaSymbolProvider.merge(session, project, it) }
            merge<FirExtensionSyntheticFunctionInterfaceProvider> { LLCombinedSyntheticFunctionSymbolProvider.merge(session, it) }
        }
    }

    /**
     * Creates a [KotlinDeclarationProvider] for the provided files if they are in the search [scope].
     *
     * Otherwise, returns `null`.
     */
    private fun createScopedDeclarationProviderForFiles(scope: GlobalSearchScope, files: List<KtFile>): KotlinDeclarationProvider? {
        if (files.isEmpty()) {
            return null
        }

        val fileProviders = buildList {
            for (file in files) {
                if (file is KtCodeFragment) {
                    // All declarations inside code fragments are local
                    continue
                }

                val virtualFile = file.virtualFile

                // 'KtFile's without a backing 'VirtualFile' can't be covered by a shadow scope, and are thus assumed in-scope.
                if (virtualFile == null || scope.contains(virtualFile)) {
                    add(KotlinFileBasedDeclarationProvider(file))
                }
            }
        }

        return KotlinCompositeDeclarationProvider.create(fileProviders)
    }
}
