package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.ide.plugins.PluginUtil
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.impl.PsiElementFinderImpl
import com.intellij.psi.impl.smartPointers.SmartTypePointerManagerImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.ObjectStubBase
import com.intellij.psi.stubs.ObjectStubSerializer
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.Stub
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.psi.stubs.StubTree
import com.intellij.psi.util.descendantsOfType
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.containers.HashingStrategy
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.dumdum.stubindex.IdeStubIndexService
import org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.platform.declarations.*
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinAlwaysAccessibleLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalModificationService
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackagePartProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderMerger
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.*
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.resolution.KaSuccessCallInfo
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.*
import org.jetbrains.kotlin.psi.stubs.elements.KtFileElementType
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
import org.jetbrains.kotlin.serialization.deserialization.ClassData

@OptIn(KaImplementationDetail::class)
fun main() {
    if (System.getProperty("java.awt.headless") == null) {
        System.setProperty("java.awt.headless", "true")
    }
    System.setProperty("idea.home.path", "/Users/jetzajac/tmp")
    Disposer.newDisposable().use { d ->
        val env = KotlinCoreEnvironment.createForProduction(d, CompilerConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val application = env.applicationEnvironment.application
        PluginStructureProvider.registerApplicationServices(application, "/META-INF/analysis-api/analysis-api-fir.xml")
        application.registerService(
            BuiltinsVirtualFileProvider::class.java,
            object : BuiltinsVirtualFileProvider() {
                override fun getBuiltinVirtualFiles(): Set<VirtualFile> {
                    TODO("Not yet implemented")
                }

                override fun createBuiltinsScope(project: Project): GlobalSearchScope {
                    TODO("Not yet implemented")
                }
            }
        )
        
        application.registerService(PluginUtil::class.java, object: PluginUtil {
            val id = PluginId.getId("dumdum")
            
            override fun getCallerPlugin(stackFrameCount: Int): PluginId? = id

            override fun findPluginId(t: Throwable): PluginId? = id

            override fun findPluginName(pluginId: PluginId): String? = id.idString

        });

        application.registerService(StubIndexService::class.java, IdeStubIndexService())

        val project = env.project as MockProject

        val singleFile = LightVirtualFile(
            "dumdum.kt",
            KotlinFileType.INSTANCE,
            "fun hello() { bar() }; fun bar() { }"
        )

        val psiFile = PsiManager.getInstance(project).findFile(singleFile)!!

        val singleModule = KaSourceModuleImpl(
            directRegularDependencies = emptyList(),
            directDependsOnDependencies = emptyList(),
            directFriendDependencies = emptyList(),
            contentScope = GlobalSearchScope.filesScope(project, listOf(singleFile)),
            targetPlatform = JvmPlatforms.defaultJvmPlatform,
            project = project,
            name = "dumdum",
            languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST),
            psiRoots = listOf(psiFile)
        )

        project.apply {
            PluginStructureProvider.registerProjectServices(project, "/META-INF/analysis-api/analysis-api-fir.xml")
            PluginStructureProvider.registerProjectListeners(project, "/META-INF/analysis-api/analysis-api-fir.xml")
            PluginStructureProvider.registerProjectExtensionPoints(project, "/META-INF/analysis-api/analysis-api-fir.xml")

            with(PsiElementFinder.EP.getPoint(project)) {
                registerExtension(JavaElementFinder(project), d)
                registerExtension(PsiElementFinderImpl(project), d)
            }

            registerService(
                KotlinDirectInheritorsProvider::class.java,
                object : KotlinDirectInheritorsProvider {
                    override fun getDirectKotlinInheritors(
                        ktClass: KtClass,
                        scope: GlobalSearchScope,
                        includeLocalInheritors: Boolean,
                    ): Iterable<KtClassOrObject> {
                        TODO("Not yet implemented")
                    }
                }
            )

            registerService(
                KotlinGlobalSearchScopeMerger::class.java,
                KotlinSimpleGlobalSearchScopeMerger()
            )

            registerService(
                SmartTypePointerManager::class.java,
                SmartTypePointerManagerImpl(project)
            )

            registerService(
                KotlinLifetimeTokenFactory::class.java,
                KotlinAlwaysAccessibleLifetimeTokenFactory()
            )

            registerService(
                KotlinPlatformSettings::class.java,
                object : KotlinPlatformSettings {
                    override val deserializedDeclarationsOrigin: KotlinDeserializedDeclarationsOrigin
                        get() = KotlinDeserializedDeclarationsOrigin.BINARIES

                }
            )

            registerService(
                KotlinProjectStructureProvider::class.java,
                object : KotlinProjectStructureProvider {
                    override fun getModule(element: PsiElement, useSiteModule: KaModule?): KaModule {
                        return singleModule
                    }
                }
            )

            registerService(
                KotlinModuleDependentsProvider::class.java,
                object : KotlinModuleDependentsProvider {
                    override fun getDirectDependents(module: KaModule): Set<KaModule> {
                        TODO("Not yet implemented")
                    }

                    override fun getTransitiveDependents(module: KaModule): Set<KaModule> {
                        TODO("Not yet implemented")
                    }

                    override fun getRefinementDependents(module: KaModule): Set<KaModule> {
                        TODO("Not yet implemented")
                    }
                }
            )

            registerService(
                KotlinModificationTrackerFactory::class.java,
                object : KotlinModificationTrackerFactory {
                    override fun createProjectWideOutOfBlockModificationTracker(): ModificationTracker {
                        return ModificationTracker.NEVER_CHANGED
                    }

                    override fun createLibrariesWideModificationTracker(): ModificationTracker {
                        return ModificationTracker.NEVER_CHANGED
                    }
                }
            )

            registerService(
                KotlinGlobalModificationService::class.java,
                object : KotlinGlobalModificationService {
                    override fun publishGlobalModuleStateModification() {
                        TODO("Not yet implemented")
                    }

                    override fun publishGlobalSourceModuleStateModification() {
                        TODO("Not yet implemented")
                    }

                    override fun publishGlobalSourceOutOfBlockModification() {
                        TODO("Not yet implemented")
                    }
                }
            )

            registerService(
                KotlinAnnotationsResolverFactory::class.java,
                object : KotlinAnnotationsResolverFactory {
                    override fun createAnnotationResolver(searchScope: GlobalSearchScope): KotlinAnnotationsResolver {
                        return object : KotlinAnnotationsResolver {
                            override fun declarationsByAnnotation(annotationClassId: ClassId): Set<KtAnnotated> {
                                TODO("Not yet implemented")
                            }

                            override fun annotationsOnDeclaration(declaration: KtAnnotated): Set<ClassId> {
                                TODO("Not yet implemented")
                            }
                        }
                    }
                }
            )

            registerService(
                KotlinResolutionScopeProvider::class.java,
                object : KotlinResolutionScopeProvider {
                    override fun getResolutionScope(module: KaModule): GlobalSearchScope {
                        return GlobalSearchScope.filesScope(project, listOf(singleFile))
                    }
                }
            )

            registerService(
                KotlinDeclarationProviderFactory::class.java,
                object : KotlinDeclarationProviderFactory {
                    override fun createDeclarationProvider(
                        scope: GlobalSearchScope,
                        contextualModule: KaModule?,
                    ): KotlinDeclarationProvider {
                        return KotlinFileBasedDeclarationProvider(psiFile as KtFile)
                    }
                }
            )
            registerService(
                KotlinDeclarationProviderMerger::class.java,
                object : KotlinDeclarationProviderMerger {
                    override fun merge(providers: List<KotlinDeclarationProvider>): KotlinDeclarationProvider {
                        TODO("Not yet implemented")
                    }
                }
            )
            registerService(
                KotlinPackageProviderFactory::class.java,
                object : KotlinPackageProviderFactory {
                    override fun createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider {
                        return object : KotlinPackageProvider {
                            override fun doesPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean {
                                TODO("Not yet implemented")
                            }

                            override fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean {
                                TODO("Not yet implemented")
                            }

                            override fun doesPlatformSpecificPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean {
                                TODO("Not yet implemented")
                            }

                            override fun getSubPackageFqNames(
                                packageFqName: FqName,
                                platform: TargetPlatform,
                                nameFilter: (Name) -> Boolean,
                            ): Set<Name> {
                                TODO("Not yet implemented")
                            }

                            override fun getKotlinOnlySubPackagesFqNames(packageFqName: FqName, nameFilter: (Name) -> Boolean): Set<Name> {
                                TODO("Not yet implemented")
                            }

                            override fun getPlatformSpecificSubPackagesFqNames(
                                packageFqName: FqName,
                                platform: TargetPlatform,
                                nameFilter: (Name) -> Boolean,
                            ): Set<Name> {
                                TODO("Not yet implemented")
                            }

                        }
                    }
                }
            )
            registerService(
                KotlinPackageProviderMerger::class.java,
                object : KotlinPackageProviderMerger {
                    override fun merge(providers: List<KotlinPackageProvider>): KotlinPackageProvider {
                        TODO("Not yet implemented")
                    }
                }
            )

            registerService(
                KotlinPackagePartProviderFactory::class.java,
                object : KotlinPackagePartProviderFactory {
                    override fun createPackagePartProvider(scope: GlobalSearchScope): PackagePartProvider {
                        return object : PackagePartProvider {
                            override fun findPackageParts(packageFqName: String): List<String> {
                                TODO("Not yet implemented")
                            }

                            override fun computePackageSetWithNonClassDeclarations(): Set<String> {
                                TODO("Not yet implemented")
                            }

                            override fun getAnnotationsOnBinaryModule(moduleName: String): List<ClassId> {
                                TODO("Not yet implemented")
                            }

                            override fun getAllOptionalAnnotationClasses(): List<ClassData> {
                                TODO("Not yet implemented")
                            }

                            override fun mayHaveOptionalAnnotationClasses(): Boolean {
                                TODO("Not yet implemented")
                            }
                        }
                    }
                }
            )
        }


        val call = psiFile.descendantsOfType<KtCallElement>().single()
        analyze(call) {
            println((call.resolveToCall() as KaSuccessCallInfo).call)
        }

        val fileElementType = KtFileElementType.INSTANCE
        // let's build stub for my file:
        val stub = fileElementType.builder.buildStubTree(psiFile)
        println(stub.childrenStubs)

        val map = StubTree(stub as KotlinFileStub).indexStubTree { indexKey ->
            HashingStrategy.canonical()
        }
        println(map)
    }
}


fun Stub.serializer(): ObjectStubSerializer<*, *> =
    when (this) {
        is PsiFileStub<*> -> type
        else -> stubType
    }

internal data class KaSourceModuleImpl(
    override val directRegularDependencies: List<KaModule>,
    override val directDependsOnDependencies: List<KaModule>,
    override val directFriendDependencies: List<KaModule>,
    override val contentScope: GlobalSearchScope,
    override val targetPlatform: TargetPlatform,
    override val project: Project,
    override val name: String,
    override val languageVersionSettings: LanguageVersionSettings,
    @KaExperimentalApi
    override val psiRoots: List<PsiFileSystemItem>,
    override val transitiveDependsOnDependencies: List<KaModule> = emptyList(),
) : KaSourceModule {
    @KaExperimentalApi
    override val stableModuleName: String? get() = name
}


private inline fun Disposable.use(block: (Disposable) -> Unit) {
    try {
        block(this)
    } finally {
        Disposer.dispose(this)
    }
}
