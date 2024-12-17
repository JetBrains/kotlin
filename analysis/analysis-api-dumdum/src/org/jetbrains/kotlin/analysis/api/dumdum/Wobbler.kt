package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.dumdum.index.*
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.JavaCoreProjectEnvironment
import com.intellij.ide.plugins.PluginUtil
import com.intellij.lang.java.JavaParserDefinition
import com.intellij.lang.jvm.facade.JvmElementProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.TransactionGuardImpl
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.roots.FileIndexFacade
import com.intellij.openapi.roots.PackageIndex
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSet
import com.intellij.openapi.vfs.VirtualFileSetFactory
import com.intellij.pom.java.InternalPersistentJavaLanguageLevelReaderService
import com.intellij.psi.*
import com.intellij.psi.impl.JavaClassSupersImpl
import com.intellij.psi.impl.PsiElementFinderImpl
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.impl.file.impl.JavaFileManager
import com.intellij.psi.impl.smartPointers.SmartTypePointerManagerImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubTreeLoader
import com.intellij.psi.util.JavaClassSupers
import com.intellij.psi.util.descendantsOfType
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names.*
import org.jetbrains.kotlin.analysis.api.dumdum.filesystem.WobblerVirtualFile
import org.jetbrains.kotlin.analysis.api.dumdum.index.*
import org.jetbrains.kotlin.analysis.api.dumdum.stubindex.*
import org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderMerger
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDirectInheritorsProvider
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinAlwaysAccessibleLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalModificationService
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackagePartProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderMerger
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.*
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaSuccessCallInfo
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProviderCliImpl
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentMode
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.utils.PathUtil

interface Wobbler {
    interface WobblerProject {
        val project: Project

        interface Analyzer {

        }

        fun <T> withAnalyzer(
            index: Index,
            singleModule: KaSourceModule,
            virtualFileFactory: VirtualFileFactory,
            body: Analyzer.() -> T,
        ): T
    }

    fun <T> withProject(body: WobblerProject.() -> T): T

    val fileBasedIndexExtensions: FileBasedIndexExtensions
    val stubIndexExtensions: StubIndexExtensions
    val stubSerializersTable: StubSerializersTable
}

class StubIndexProjectService(val stubIndex: StubIndex)

@OptIn(KaImplementationDetail::class)
fun <T> withWobbler(f: Wobbler.() -> T): T {
    if (System.getProperty("java.awt.headless") == null) {
        System.setProperty("java.awt.headless", "true")
    }
    System.setProperty("idea.home.path", "/Users/jetzajac/tmp")
    setupIdeaStandaloneExecution()
    return Disposer.newDisposable().use { applicationDisposable ->
        val applicationEnvironment = KotlinCoreApplicationEnvironment.create(
            applicationDisposable,
            KotlinCoreApplicationEnvironmentMode.Production
        ).apply {
            registerFileType(KotlinFileType.INSTANCE, "kt")
            registerFileType(KotlinFileType.INSTANCE, KotlinParserDefinition.STD_SCRIPT_SUFFIX)
            registerParserDefinition(KotlinParserDefinition())
            application.run {
                registerService(KotlinBinaryClassCache::class.java, KotlinBinaryClassCache())
                registerService(JavaClassSupers::class.java, JavaClassSupersImpl::class.java)
                registerService(TransactionGuard::class.java, TransactionGuardImpl::class.java)
                registerService(VirtualFileSetFactory::class.java, object : VirtualFileSetFactory {
                    override fun createCompactVirtualFileSet(): VirtualFileSet =
                        VirtualFileSetImpl(mutableSetOf())

                    override fun createCompactVirtualFileSet(files: MutableCollection<out VirtualFile>): VirtualFileSet =
                        VirtualFileSetImpl(files.toMutableSet())
                })
                registerService(
                    InternalPersistentJavaLanguageLevelReaderService::class.java,
                    InternalPersistentJavaLanguageLevelReaderService.DefaultImpl()
                )
                registerService(
                    BuiltinsVirtualFileProvider::class.java,
                    BuiltinsVirtualFileProviderCliImpl()
                )

                registerService(
                    StubTreeLoader::class.java,
                    StubTreeLoaderImpl::class.java
                )

                registerService(PluginUtil::class.java, object : PluginUtil {
                    val id = PluginId.getId("dumdum")

                    override fun getCallerPlugin(stackFrameCount: Int): PluginId? = id

                    override fun findPluginId(t: Throwable): PluginId? = id

                    override fun findPluginName(pluginId: PluginId): String? = id.idString

                });

                registerService(StubIndexService::class.java, IdeStubIndexService())
            }

            registerFileType(PlainTextFileType.INSTANCE, "xml")
            registerParserDefinition(JavaParserDefinition())

            //                registerApplicationExtensionPointsAndExtensionsFrom(configuration, "extensions/compiler.xml")
            CoreApplicationEnvironment.registerExtensionPointAndExtensions(
                PathUtil.getResourcePathForClass(ExitCode::class.java).toPath(),
                "extensions/compiler.xml",
                application.extensionArea
            )

            PluginStructureProvider.registerApplicationServices(application, "/META-INF/analysis-api/analysis-api-fir.xml")
        }

        val fileBasedIndexExtensions = fileBasedIndexExtensions(
            listOf(
                KotlinJvmModuleAnnotationsIndex(),
                KotlinModuleMappingIndex(),
                KotlinPartialPackageNamesIndex(),
                KotlinTopLevelCallableByPackageShortNameIndex(),
                KotlinTopLevelClassLikeDeclarationByPackageShortNameIndex(),
            )
        )

        val stubIndexExtensions = stubIndexExtensions(
            stubIndexExtensions = listOf(
                KotlinAnnotationsIndex.Helper,
                KotlinClassShortNameIndex.Helper,
                KotlinExtensionsInObjectsByReceiverTypeIndex.Helper,
                KotlinFileFacadeClassByPackageIndex.Helper,
                KotlinFileFacadeFqNameIndex.Helper,
                KotlinFileFacadeShortNameIndex.Helper,
                KotlinFilePartClassIndex.Helper,
                KotlinFullClassNameIndex.Helper,
                KotlinFunctionShortNameIndex.Helper,
                KotlinInnerTypeAliasClassIdIndex.Helper,
                KotlinJvmNameAnnotationIndex.Helper,
                KotlinMultiFileClassPartIndex.Helper,
                KotlinOverridableInternalMembersShortNameIndex.Helper,
                KotlinPrimeSymbolNameIndex.Helper,
                KotlinProbablyContractedFunctionShortNameIndex.Helper,
                KotlinProbablyNothingFunctionShortNameIndex.Helper,
                KotlinProbablyNothingPropertyShortNameIndex.Helper,
                KotlinPropertyShortNameIndex.Helper,
                KotlinScriptFqnIndex.Helper,
                KotlinSubclassObjectNameIndex.Helper,
                KotlinSuperClassIndex.Helper,
                KotlinTopLevelClassByPackageIndex.Helper,
                KotlinTopLevelExpectFunctionFqNameIndex.Helper,
                KotlinTopLevelExpectPropertyFqNameIndex.Helper,
                KotlinTopLevelExtensionsByReceiverTypeIndex.Helper,
                KotlinTopLevelFunctionByPackageIndex.Helper,
                KotlinTopLevelFunctionFqnNameIndex.Helper,
                KotlinTopLevelPropertyByPackageIndex.Helper,
                KotlinTopLevelPropertyFqnNameIndex.Helper,
                KotlinTopLevelTypeAliasByPackageIndex.Helper,
                KotlinTopLevelTypeAliasFqNameIndex.Helper,
                KotlinTypeAliasByExpansionShortNameIndex.Helper,
                KotlinTypeAliasShortNameIndex.Helper,
                KotlinExactPackagesIndex.Helper,
            ),
        )

        val stubSerializersTable = stubSerializersTable()

        object : Wobbler {
            override fun <T> withProject(body: Wobbler.WobblerProject.() -> T): T =
                Disposer.newDisposable(applicationDisposable).use { projectDisposable ->
                    val projectEnvironment = object : JavaCoreProjectEnvironment(projectDisposable, applicationEnvironment) {
                        override fun createCoreFileManager(): JavaFileManager {
                            return JavaFileManagerImpl()
                        }

                        override fun createCorePackageIndex(): PackageIndex {
                            return PackageIndexImpl()
                        }

                        override fun createFileIndexFacade(): FileIndexFacade {
                            return FileIndexFacadeImpl(project)
                        }
                    }
                    object : Wobbler.WobblerProject {
                        override val project: Project
                            get() = projectEnvironment.project

                        override fun <T> withAnalyzer(
                            index: Index,
                            singleModule: KaSourceModule,
                            virtualFileFactory: VirtualFileFactory,
                            body: Wobbler.WobblerProject.Analyzer.() -> T,
                        ): T {
                            val psiManager = PsiManager.getInstance(project)

                            val stubIndex: StubIndex = index.stubIndex(
                                stubIndexExtensions = stubIndexExtensions,
                                virtualFileFactory = virtualFileFactory,
                                documentIdMapper = { virtualFile ->
                                    (virtualFile as WobblerVirtualFile).fileId
                                },
                                psiFileFactory = { psiManager.findFile(it)!! },
                                stubSerializersTable = stubSerializersTable,
                            )

                            val fileBasedIndex: FileBasedIndex = index.fileBased(
                                virtualFileFactory = virtualFileFactory,
                                fileBasedIndexExtensions = fileBasedIndexExtensions
                            )
                            val project = projectEnvironment.project

                            project.apply {
                                registerService(
                                    JavaModuleResolver::class.java,
                                    object : JavaModuleResolver {
                                        override fun checkAccessibility(
                                            fileFromOurModule: VirtualFile?,
                                            referencedFile: VirtualFile,
                                            referencedPackage: FqName?,
                                        ): JavaModuleResolver.AccessError? {
                                            return null
                                        }

                                        override fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<JavaAnnotation>? {
                                            TODO("Not yet implemented")
                                        }

                                    }
                                )

                                CoreApplicationEnvironment.registerExtensionPoint(
                                    project.extensionArea,
                                    PsiTreeChangePreprocessor.EP.name,
                                    PsiTreeChangePreprocessor::class.java
                                )
                                CoreApplicationEnvironment.registerExtensionPoint(
                                    project.extensionArea,
                                    PsiElementFinder.EP.name,
                                    PsiElementFinder::class.java
                                )
                                CoreApplicationEnvironment.registerExtensionPoint(
                                    project.extensionArea,
                                    JvmElementProvider.EP_NAME,
                                    JvmElementProvider::class.java
                                )

                                PluginStructureProvider.registerProjectServices(project, "/META-INF/analysis-api/analysis-api-fir.xml")
                                PluginStructureProvider.registerProjectListeners(project, "/META-INF/analysis-api/analysis-api-fir.xml")
                                PluginStructureProvider.registerProjectExtensionPoints(
                                    project,
                                    "/META-INF/analysis-api/analysis-api-fir.xml"
                                )

                                with(PsiElementFinder.EP.getPoint(project)) {
                                    registerExtension(JavaElementFinder(project), applicationDisposable)
                                    registerExtension(PsiElementFinderImpl(project), applicationDisposable)
                                }

                                registerService(
                                    StubIndexProjectService::class.java,
                                    StubIndexProjectService(stubIndex)
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
                                    KotlinAnnotationsResolverFactory::class.java,
                                    IdeKotlinAnnotationsResolverFactory(project, stubIndex),
                                )

                                registerService(
                                    KotlinResolutionScopeProvider::class.java,
                                    KotlinByModulesResolutionScopeProvider()
                                )

                                registerService(
                                    KotlinDeclarationProviderFactory::class.java,
                                    IdeKotlinDeclarationProviderFactory(project, stubIndex, fileBasedIndex)
                                )
                                registerService(
                                    KotlinDeclarationProviderMerger::class.java,
                                    IdeKotlinDeclarationProviderMerger(project, stubIndex, fileBasedIndex)
                                )
                                registerService(
                                    KotlinPackageProviderFactory::class.java,
                                    IdeKotlinPackageProviderFactory(project, fileBasedIndex)
                                )
                                registerService(
                                    KotlinPackageProviderMerger::class.java,
                                    IdeKotlinPackageProviderMerger(project, fileBasedIndex)
                                )

                                registerService(
                                    KotlinPackagePartProviderFactory::class.java,
                                    IdeKotlinPackagePartProviderFactory(fileBasedIndex)
                                )
                            }

                            return object : Wobbler.WobblerProject.Analyzer {

                            }.body()
                        }


                    }.body()
                }

            override val fileBasedIndexExtensions: FileBasedIndexExtensions
                get() = fileBasedIndexExtensions
            override val stubIndexExtensions: StubIndexExtensions
                get() = stubIndexExtensions
            override val stubSerializersTable: StubSerializersTable
                get() = stubSerializersTable
        }.f()
    }
}

private inline fun <T> Disposable.use(block: (Disposable) -> T): T =
    try {
        block(this)
    } finally {
        Disposer.dispose(this)
    }
