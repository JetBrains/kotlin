/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinFileBasedDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.createAnnotationResolver
import org.jetbrains.kotlin.analysis.api.platform.declarations.createDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinAnchorModuleProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.platform.utils.mergeInto
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.analysis.api.utils.errors.withKaModuleEntry
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirGlobalResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirLazyDeclarationResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.*
import org.jetbrains.kotlin.assignment.plugin.AssignmentCommandLineProcessor
import org.jetbrains.kotlin.assignment.plugin.AssignmentConfigurationKeys
import org.jetbrains.kotlin.assignment.plugin.k2.FirAssignmentPluginExtensionRegistrar
import org.jetbrains.kotlin.cli.plugins.processCompilerPluginsOptions
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.fir.analysis.extensions.additionalCheckers
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.builder.Context
import org.jetbrains.kotlin.fir.builder.FirReplSnippetConfiguratorExtension
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirFileBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirReplSnippetBuilder
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirExtensionSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.syntheticFunctionInterfacesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.resolve.transformers.FirDummyCompilerLazyDeclarationResolver
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.scripting.compiler.plugin.FirScriptingSamWithReceiverExtensionRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.makeScriptCompilerArguments
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.FirReplHistoryScope
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import org.jetbrains.kotlin.utils.exceptions.withVirtualFileEntry
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class FirReplSnippetConfiguratorExtensionImpl(
    session: FirSession,
    // TODO: left here because it seems it will be needed soon, remove supression if used or remove the param if it is not the case
    @Suppress("UNUSED_PARAMETER", "unused") hostConfiguration: ScriptingHostConfiguration,
) : FirReplSnippetConfiguratorExtension(session) {

    override fun isReplSnippetsSource(sourceFile: KtSourceFile?, scriptSource: KtSourceElement): Boolean {
        return (sourceFile?.name?.endsWith(".jupyter.kts") == true)
    }

    override fun FirReplSnippetBuilder.configureContainingFile(fileBuilder: FirFileBuilder) {
    }

    override fun FirReplSnippetBuilder.configure(sourceFile: KtSourceFile?, context: Context<PsiElement>) {
    }

    companion object {
        fun getFactory(hostConfiguration: ScriptingHostConfiguration): Factory {
            return Factory { session -> FirReplSnippetConfiguratorExtensionImpl(session, hostConfiguration) }
        }
    }
}

class FirReplSnippetResolveExtensionImpl(
    session: FirSession,
    // TODO: left here because it seems it will be needed soon, remove suppression if used or remove the param if it is not the case
    @Suppress("UNUSED_PARAMETER", "unused") hostConfiguration: ScriptingHostConfiguration,
) : FirReplSnippetResolveExtension(session) {

    private val replHistoryProvider: FirReplHistoryProvider by lazy {
        session.moduleData.dependencies.firstOrNull()?.session?.replHistoryProvider ?: error("No repl history provider found")
    }

    @OptIn(SymbolInternals::class)
    override fun getSnippetScope(currentSnippet: FirReplSnippet, useSiteSession: FirSession): FirScope? {
        // TODO: consider caching (KT-72975)
        val properties = HashMap<Name, FirVariableSymbol<*>>()
        val functions = HashMap<Name, ArrayList<FirNamedFunctionSymbol>>() // TODO: find out how overloads should work
        val classLikes = HashMap<Name, FirClassLikeSymbol<*>>()
        replHistoryProvider.getSnippets().forEach { snippet ->
            if (currentSnippet == snippet) return@forEach
            snippet.fir.body.statements.forEach {
                when (it) {
                    is FirProperty -> properties.put(it.name, it.symbol)
                    is FirSimpleFunction -> functions.getOrPut(it.name, { ArrayList() }).add(it.symbol)
                    is FirRegularClass -> classLikes.put(it.name, it.symbol)
                    is FirTypeAlias -> classLikes.put(it.name, it.symbol)
                }
            }
        }
        return FirReplHistoryScope(properties, functions, classLikes, useSiteSession)
    }

    override fun updateResolved(snippet: FirReplSnippet) {
        replHistoryProvider.putSnippet(snippet.symbol)
    }

    companion object {
        fun getFactory(hostConfiguration: ScriptingHostConfiguration): Factory {
            return Factory { session -> FirReplSnippetResolveExtensionImpl(session, hostConfiguration) }
        }
    }
}

class FirReplCompilerExtensionRegistrar(
    private val hostConfiguration: ScriptingHostConfiguration
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        +FirReplSnippetConfiguratorExtensionImpl.getFactory(hostConfiguration)
        +FirReplSnippetResolveExtensionImpl.getFactory(hostConfiguration)
    }
}

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
internal abstract class LLFirAbstractSessionFactory(protected val project: Project) {
    private val globalResolveComponents: LLFirGlobalResolveComponents
        get() = LLFirGlobalResolveComponents.getInstance(project)

    abstract fun createSourcesSession(module: KaSourceModule): LLFirSourcesSession
    abstract fun createLibrarySession(module: KaModule): LLFirLibraryOrLibrarySourceResolvableModuleSession
    abstract fun createBinaryLibrarySession(module: KaLibraryModule): LLFirLibrarySession

    private fun createLibraryProvidersForScope(
        session: LLFirSession,
        scope: GlobalSearchScope,
        builtinSymbolProvider: FirSymbolProvider,
    ): LLFirModuleWithDependenciesSymbolProvider {
        return LLFirModuleWithDependenciesSymbolProvider(
            session,
            providers = createProjectLibraryProvidersForScope(session, scope),
            LLFirDependenciesSymbolProvider(session) {
                buildList {
                    addAll(collectDependencySymbolProviders(session.ktModule))
                    add(builtinSymbolProvider)
                }
            },
        )
    }

    abstract fun createProjectLibraryProvidersForScope(
        session: LLFirSession,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean = false,
    ): List<FirSymbolProvider>

    fun createScriptSession(module: KaScriptModule): LLFirScriptSession {
        val platform = module.targetPlatform
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform)
        val languageVersionSettings = wrapLanguageVersionSettings(module.languageVersionSettings)
        val scopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)

        val components = LLFirModuleResolveComponents(module, globalResolveComponents, scopeProvider)

        val session = LLFirScriptSession(module, components, builtinsSession.builtinTypes)
        components.session = session

        val moduleData = createModuleData(session)

        return session.apply {
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            registerAllCommonComponents(languageVersionSettings)

            registerCommonComponentsAfterExtensionsAreConfigured()
            registerJavaComponents(JavaModuleResolver.getInstance(project))


            val provider = LLFirProvider(
                this,
                components,
                canContainKotlinPackage = true,
            ) { scope ->
                scope.createScopedDeclarationProviderForFile(module.file)
            }

            register(FirProvider::class, provider)
            register(FirLazyDeclarationResolver::class, LLFirLazyDeclarationResolver())

            val dependencyProvider = LLFirDependenciesSymbolProvider(this) {
                buildList {
                    addMerged(session, collectDependencySymbolProviders(module))
                    add(builtinsSession.symbolProvider)
                }
            }

            val javaSymbolProvider = LLFirJavaSymbolProvider(this, provider.searchScope)
            register(JavaSymbolProvider::class, javaSymbolProvider)

            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
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

            registerScriptExtensions(this, module.file)

            LLFirSessionConfigurator.configure(this)
        }
    }

    @OptIn(ExperimentalCompilerApi::class)
    private fun registerScriptExtensions(session: LLFirSession, file: KtFile) {
        FirSessionConfigurator(session).apply {
            val hostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {}
            val scriptDefinition = file.findScriptDefinition()
                ?: errorWithAttachment("Cannot load script definition") {
                    withVirtualFileEntry("file", file.virtualFile)
                }

            val compilerArguments = makeScriptCompilerArguments(scriptDefinition.compilerOptions.toList())
            val commandLineProcessors = listOf(AssignmentCommandLineProcessor())
            val compilerConfiguration = CompilerConfiguration()
            processCompilerPluginsOptions(
                compilerConfiguration, compilerArguments.pluginOptions?.asIterable() ?: emptyList(), commandLineProcessors
            )

            val extensionRegistrar = FirScriptingCompilerExtensionIdeRegistrar(
                project,
                hostConfiguration,
                scriptDefinitionSources = emptyList(),
                scriptDefinitions = listOf(scriptDefinition)
            )

            registerExtensions(extensionRegistrar.configure())
            registerExtensions(FirScriptingSamWithReceiverExtensionRegistrar().configure())
            // -- Only for testing: Begin --
            registerExtensions(FirReplCompilerExtensionRegistrar(hostConfiguration).configure())
            // -- Only for testing: End --
            compilerConfiguration.getList(AssignmentConfigurationKeys.ANNOTATION).takeIf { it.isNotEmpty() }?.let {
                registerExtensions(FirAssignmentPluginExtensionRegistrar(it).configure())
            }
        }.configure()
    }

    fun createNotUnderContentRootResolvableSession(module: KaNotUnderContentRootModule): LLFirNotUnderContentRootResolvableModuleSession {
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(JvmPlatforms.unspecifiedJvmPlatform)
        val languageVersionSettings = KotlinProjectStructureProvider.getInstance(project).globalLanguageVersionSettings
        val scopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
        val components = LLFirModuleResolveComponents(module, globalResolveComponents, scopeProvider)

        val session = LLFirNotUnderContentRootResolvableModuleSession(module, components, builtinsSession.builtinTypes)
        components.session = session

        val moduleData = createModuleData(session)

        return session.apply {
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            registerAllCommonComponents(languageVersionSettings)

            registerCommonComponentsAfterExtensionsAreConfigured()
            registerJavaComponents(JavaModuleResolver.getInstance(project))


            val ktFile = module.file as? KtFile

            val provider = LLFirProvider(
                this,
                components,
                canContainKotlinPackage = true,
            ) { scope ->
                ktFile?.let { scope.createScopedDeclarationProviderForFile(it) }
            }

            register(FirProvider::class, provider)
            register(FirLazyDeclarationResolver::class, LLFirLazyDeclarationResolver())

            val dependencyProvider = LLFirDependenciesSymbolProvider(this) { listOf(builtinsSession.symbolProvider) }

            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
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

    protected class SourceSessionCreationContext(
        val contentScope: GlobalSearchScope,
        val firProvider: LLFirProvider,
        val dependencyProvider: LLFirDependenciesSymbolProvider,
        val syntheticFunctionInterfaceProvider: FirExtensionSyntheticFunctionInterfaceProvider?,
        val switchableExtensionDeclarationsSymbolProvider: FirSwitchableExtensionDeclarationsSymbolProvider?,
    )

    protected fun doCreateSourcesSession(
        module: KaSourceModule,
        scopeProvider: FirKotlinScopeProvider = FirKotlinScopeProvider(),
        additionalSessionConfiguration: LLFirSourcesSession.(context: SourceSessionCreationContext) -> Unit,
    ): LLFirSourcesSession {
        val platform = module.targetPlatform
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform)
        val languageVersionSettings = wrapLanguageVersionSettings(module.languageVersionSettings)

        val components = LLFirModuleResolveComponents(module, globalResolveComponents, scopeProvider)

        val session = LLFirSourcesSession(module, components, builtinsSession.builtinTypes)
        components.session = session

        val moduleData = createModuleData(session)

        return session.apply {
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            registerAllCommonComponents(languageVersionSettings)

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

            registerCompilerPluginServices(project, module)
            registerCompilerPluginExtensions(project, module)
            registerCommonComponentsAfterExtensionsAreConfigured()

            val dependencyProvider = LLFirDependenciesSymbolProvider(this) {
                buildList {
                    addMerged(session, collectDependencySymbolProviders(module))
                    add(builtinsSession.symbolProvider)
                }
            }

            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)
            register(LLFirFirClassByPsiClassProvider::class, LLFirFirClassByPsiClassProvider(this))

            LLFirSessionConfigurator.configure(this)

            extensionService.additionalCheckers.forEach(session.checkersComponent::register)

            val syntheticFunctionInterfaceProvider =
                FirExtensionSyntheticFunctionInterfaceProvider.createIfNeeded(this, moduleData, scopeProvider)
            val switchableExtensionDeclarationsSymbolProvider =
                LLFirSwitchableExtensionDeclarationsSymbolProvider.createIfNeeded(this)?.also {
                    register(FirSwitchableExtensionDeclarationsSymbolProvider::class, it)
                }

            val context = SourceSessionCreationContext(
                firProvider.searchScope, firProvider, dependencyProvider, syntheticFunctionInterfaceProvider,
                switchableExtensionDeclarationsSymbolProvider,
            )

            register(
                FirBuiltinSyntheticFunctionInterfaceProvider::class,
                builtinsSession.syntheticFunctionInterfacesSymbolProvider
            )

            additionalSessionConfiguration(context)
        }
    }

    protected class LibrarySessionCreationContext(
        val contentScope: GlobalSearchScope,
        val firProvider: LLFirProvider,
        val dependencyProvider: LLFirDependenciesSymbolProvider,
    )

    protected fun doCreateLibrarySession(
        module: KaModule,
        additionalSessionConfiguration: LLFirLibraryOrLibrarySourceResolvableModuleSession.(context: LibrarySessionCreationContext) -> Unit,
    ): LLFirLibraryOrLibrarySourceResolvableModuleSession {
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

        return session.apply {
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            registerAllCommonComponents(languageVersionSettings)
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

            val contentScope = (binaryModule as? KaLibraryModule)?.contentScope ?: firProvider.searchScope

            // We need FirRegisteredPluginAnnotations during extensions' registration process
            val annotationsResolver = project.createAnnotationResolver(contentScope)
            register(FirRegisteredPluginAnnotations::class, LLFirIdeRegisteredPluginAnnotations(this, annotationsResolver))
            register(FirPredicateBasedProvider::class, FirEmptyPredicateBasedProvider)

            val dependencyProvider = LLFirDependenciesSymbolProvider(this) {
                buildList {
                    if (module !is KaBuiltinsModule) {
                        add(builtinsSession.symbolProvider)
                    }

                    // Script dependencies are self-contained and should not depend on other libraries
                    if (module !is KaScriptDependencyModule) {
                        // Add all libraries excluding the current one
                        val librariesSearchScope = ProjectScope.getLibrariesScope(project)
                            .intersectWith(GlobalSearchScope.notScope(binaryModule.contentScope))

                        val restLibrariesProvider = createProjectLibraryProvidersForScope(
                            session,
                            librariesSearchScope,
                            isFallbackDependenciesProvider = true,
                        )

                        addAll(restLibrariesProvider)

                        if (binaryModule is KaLibraryModule) {
                            KotlinAnchorModuleProvider.getInstance(project)?.getAnchorModule(binaryModule)?.let { anchorModule ->
                                val anchorModuleSession = LLFirSessionCache.getInstance(project).getSession(anchorModule)
                                val anchorModuleSymbolProvider =
                                    anchorModuleSession.symbolProvider as LLFirModuleWithDependenciesSymbolProvider

                                addAll(anchorModuleSymbolProvider.providers)
                                addAll(anchorModuleSymbolProvider.dependencyProvider.providers)
                            }
                        }
                    }
                }
            }

            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)
            register(LLFirFirClassByPsiClassProvider::class, LLFirFirClassByPsiClassProvider(this))

            val context = LibrarySessionCreationContext(contentScope, firProvider, dependencyProvider)
            additionalSessionConfiguration(context)

            LLFirSessionConfigurator.configure(this)
        }
    }

    protected class BinaryLibrarySessionCreationContext

    protected fun doCreateBinaryLibrarySession(
        module: KaLibraryModule,
        additionalSessionConfiguration: LLFirLibrarySession.(context: BinaryLibrarySessionCreationContext) -> Unit,
    ): LLFirLibrarySession {
        val platform = module.targetPlatform
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform)

        val session = LLFirLibrarySession(module, builtinsSession.builtinTypes)

        val moduleData = createModuleData(session)

        return session.apply {
            val languageVersionSettings = KotlinProjectStructureProvider.getInstance(project).libraryLanguageVersionSettings
            registerModuleData(moduleData)
            registerIdeComponents(project, languageVersionSettings)
            register(FirLazyDeclarationResolver::class, FirDummyCompilerLazyDeclarationResolver)
            registerCommonComponents(languageVersionSettings)
            registerCommonComponentsAfterExtensionsAreConfigured()
            registerDefaultComponents()

            val kotlinScopeProvider = when {
                platform.isJvm() -> FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
                else -> FirKotlinScopeProvider()
            }

            register(FirKotlinScopeProvider::class, kotlinScopeProvider)

            val symbolProvider = createLibraryProvidersForScope(
                this,
                module.contentScope,
                builtinsSession.symbolProvider
            )

            register(LLFirFirClassByPsiClassProvider::class, LLFirFirClassByPsiClassProvider(this))
            register(FirProvider::class, LLFirLibrarySessionProvider(symbolProvider))
            register(FirSymbolProvider::class, symbolProvider)

            val context = BinaryLibrarySessionCreationContext()
            additionalSessionConfiguration(context)
            LLFirSessionConfigurator.configure(this)
        }
    }

    abstract fun createDanglingFileSession(module: KaDanglingFileModule, contextSession: LLFirSession): LLFirSession

    protected fun doCreateDanglingFileSession(
        module: KaDanglingFileModule,
        contextSession: LLFirSession,
        additionalSessionConfiguration: LLFirDanglingFileSession.(DanglingFileSessionCreationContext) -> Unit,
    ): LLFirSession {
        val danglingFile = module.file
        val platform = module.targetPlatform

        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform)
        val languageVersionSettings = wrapLanguageVersionSettings(contextSession.languageVersionSettings)
        val scopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)

        val components = LLFirModuleResolveComponents(module, globalResolveComponents, scopeProvider)

        val session = LLFirDanglingFileSession(module, components, builtinsSession.builtinTypes, danglingFile.modificationStamp)
        components.session = session

        val moduleData = createModuleData(session)

        return session.apply {
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            registerAllCommonComponents(languageVersionSettings)

            val firProvider = LLFirProvider(
                this,
                components,
                canContainKotlinPackage = true,
                disregardSelfDeclarations = module.resolutionMode == KaDanglingFileResolutionMode.IGNORE_SELF,
                declarationProviderFactory = { scope -> scope.createScopedDeclarationProviderForFile(danglingFile) }
            )

            register(FirProvider::class, firProvider)
            register(FirLazyDeclarationResolver::class, LLFirLazyDeclarationResolver())

            val contextModule = module.contextModule
            if (contextModule is KaSourceModule) {
                registerCompilerPluginServices(project, contextModule)
                registerCompilerPluginExtensions(project, contextModule)
            } else {
                register(FirRegisteredPluginAnnotations::class, FirRegisteredPluginAnnotationsImpl(this))
                register(FirPredicateBasedProvider::class, FirEmptyPredicateBasedProvider)
            }

            registerCommonComponentsAfterExtensionsAreConfigured()

            val dependencyProvider = LLFirDependenciesSymbolProvider(this) {
                buildList {
                    addMerged(session, computeFlattenedSymbolProviders(listOf(contextSession)))

                    when (contextSession.ktModule) {
                        is KaLibraryModule, is KaLibrarySourceModule -> {
                            // Wrap library dependencies into a single classpath-filtering provider
                            // Also see 'LLFirDanglingFileDependenciesSymbolProvider.filterSymbols()'
                            add(LLFirDanglingFileDependenciesSymbolProvider(contextSession.dependenciesSymbolProvider))
                        }
                        else -> add(contextSession.dependenciesSymbolProvider)
                    }

                    add(builtinsSession.symbolProvider)
                }
            }

            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)
            register(LLFirFirClassByPsiClassProvider::class, LLFirFirClassByPsiClassProvider(this))

            LLFirSessionConfigurator.configure(this)

            extensionService.additionalCheckers.forEach(session.checkersComponent::register)

            val syntheticFunctionInterfaceProvider = FirExtensionSyntheticFunctionInterfaceProvider
                .createIfNeeded(this, moduleData, scopeProvider)

            val switchableExtensionDeclarationsSymbolProvider = LLFirSwitchableExtensionDeclarationsSymbolProvider
                .createIfNeeded(this)
                ?.also { register(FirSwitchableExtensionDeclarationsSymbolProvider::class, it) }

            if (contextModule is KaScriptModule) {
                registerScriptExtensions(this, contextModule.file)
            }

            val context = DanglingFileSessionCreationContext(
                moduleData,
                dependencyProvider,
                syntheticFunctionInterfaceProvider,
                switchableExtensionDeclarationsSymbolProvider
            )

            additionalSessionConfiguration(this, context)
        }
    }

    protected class DanglingFileSessionCreationContext(
        val moduleData: LLFirModuleData,
        val dependencyProvider: LLFirDependenciesSymbolProvider,
        val syntheticFunctionInterfaceProvider: FirExtensionSyntheticFunctionInterfaceProvider?,
        val switchableExtensionDeclarationsSymbolProvider: FirSwitchableExtensionDeclarationsSymbolProvider?,
    )

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
                    LanguageFeature.State.ENABLED, LanguageFeature.State.ENABLED_WITH_WARNING -> true
                    else -> false
                }
            }
        }
    }

    private fun collectDependencySymbolProviders(module: KaModule): List<FirSymbolProvider> {
        val llFirSessionCache = LLFirSessionCache.getInstance(project)

        fun getOrCreateSessionForDependency(dependency: KaModule): LLFirSession? = when (dependency) {
            is KaBuiltinsModule -> null // Built-ins are already added

            is KaLibraryModule -> llFirSessionCache.getSession(dependency, preferBinary = true)

            is KaSourceModule -> llFirSessionCache.getSession(dependency)

            is KaDanglingFileModule -> {
                requireWithAttachment(dependency.isStable, message = { "Unstable dangling modules cannot be used as a dependency" }) {
                    withKaModuleEntry("module", module)
                    withKaModuleEntry("dependency", dependency)
                    withPsiEntry("dependencyFile", dependency.file)
                }
                llFirSessionCache.getSession(dependency)
            }

            else -> {
                errorWithAttachment("Module ${module::class} cannot depend on ${dependency::class}") {
                    withKaModuleEntry("module", module)
                    withKaModuleEntry("dependency", dependency)
                }
            }
        }

        // Please update KmpModuleSorterTest#buildDependenciesToTest if the logic of collecting dependencies changes
        val dependencyModules = buildSet {
            addAll(module.directRegularDependencies)
            addAll(module.directFriendDependencies)

            // The dependency provider needs to have access to all direct and indirect `dependsOn` dependencies, as `dependsOn`
            // dependencies are transitive.
            addAll(module.transitiveDependsOnDependencies)
        }

        val orderedDependencyModules = KmpModuleSorter.order(dependencyModules.toList())

        val dependencySessions = orderedDependencyModules.mapNotNull(::getOrCreateSessionForDependency)
        return computeFlattenedSymbolProviders(dependencySessions)
    }

    private fun computeFlattenedSymbolProviders(dependencySessions: List<LLFirSession>): List<FirSymbolProvider> {
        return dependencySessions.flatMap { session ->
            when (val dependencyProvider = session.symbolProvider) {
                is LLFirModuleWithDependenciesSymbolProvider -> dependencyProvider.providers
                else -> listOf(dependencyProvider)
            }
        }
    }

    private fun createModuleData(session: LLFirSession): LLFirModuleData {
        return LLFirModuleData(session)
    }

    private fun LLFirSession.registerAllCommonComponents(languageVersionSettings: LanguageVersionSettings) {
        registerIdeComponents(project, languageVersionSettings)
        registerCommonComponents(languageVersionSettings)
        registerResolveComponents()
        registerDefaultComponents()
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
            merge<LLFirProvider.SymbolProvider> { LLFirCombinedKotlinSymbolProvider.merge(session, project, it) }
            merge<LLFirJavaSymbolProvider> { LLFirCombinedJavaSymbolProvider.merge(session, project, it) }
            merge<FirExtensionSyntheticFunctionInterfaceProvider> { LLFirCombinedSyntheticFunctionSymbolProvider.merge(session, it) }
        }
    }

    /**
     * Creates a single-file [KotlinDeclarationProvider] for the provided file, if it is in the search scope.
     *
     * Otherwise, returns `null`.
     */
    private fun GlobalSearchScope.createScopedDeclarationProviderForFile(file: KtFile): KotlinDeclarationProvider? =
        // KtFiles without a backing VirtualFile can't be covered by a shadow scope, and are thus assumed in-scope.
        if (file.virtualFile == null || contains(file.virtualFile)) {
            KotlinFileBasedDeclarationProvider(file)
        } else {
            null
        }
}
