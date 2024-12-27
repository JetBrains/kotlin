@file:OptIn(KaPlatformInterface::class, KaExperimentalApi::class)

package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.descendantsOfType
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.dumdum.index.FileId
import org.jetbrains.kotlin.analysis.api.dumdum.index.inMemoryIndex
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaErrorCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaSuccessCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.nio.file.Path

data class StringFileId(val id: String) : FileId {
    override val fileName: String get() = id
}

fun test1() {
    withWobbler {
        val files = mapOf(
            StringFileId("/src/foo/foo.kt") to """
                    
                    package foo
                    
                    import bar.Bar
                    
                    class Foo {
                        fun foo() { Bar().bar() }
                    }
                    
                    fun foo() { }
                    
                    """.trimIndent(),

            StringFileId("/src/bar/bar.kt") to """

                    package bar
                    
                    import foo.Foo
                    import foo.foo 
                    
                    class Bar {
                        fun bar() { Foo().foo() }
                    }
                    
                    fun bar() { foo() }
                    
                    """.trimIndent(),

            StringFileId("/src/bar/baz.kt") to """

                    package baz
                    
                    fun baz() { listOf(1) }
                    
                    """.trimIndent()
        )


        val index = withProject {
            val psiManager = PsiManager.getInstance(project)
            inMemoryIndex(
                files.keys.associateWith { fileId ->
                    val vFile = virtualFile(fileId) {
                        files[fileId]!!.toByteArray()
                    }
                    val psiFile = psiManager.findFile(vFile)!!
                    mapFile(psiFile)
                }
            )
        }

        val stdlibPath = File("/Users/jetzajac/Projects/kotlin/dist/kotlinc/lib/kotlin-stdlib.jar").toPath()

        files.forEach { (fileId, text) ->
            withProject {
                val binaryRoots = listOf(stdlibPath)
                val lib = KaLibraryModuleImpl(
                    directRegularDependencies = emptyList(),
                    libraryName = "dumdum-lib",
                    binaryRoots = binaryRoots,
                    binaryVirtualFiles = listOf(),
                    librarySources = null,
                    isSdk = false,
                    directDependsOnDependencies = emptyList(),
                    transitiveDependsOnDependencies = emptyList(),
                    directFriendDependencies = emptyList(),
                    contentScope = createSearchScopeByLibraryRoots(
                        binaryRoots = binaryRoots,
                        binaryVirtualFiles = listOf(),
                        project = project,
                        jarFileSystem = jarFileSystem
                    ),
                    targetPlatform = JvmPlatforms.defaultJvmPlatform,
                    project = project
                )


                val singleModule = KaSourceModuleImpl(
                    directRegularDependencies = listOf(lib),
                    directDependsOnDependencies = emptyList(),
                    directFriendDependencies = emptyList(),
                    contentScope = GlobalSearchScope.allScope(project),
                    targetPlatform = JvmPlatforms.defaultJvmPlatform,
                    project = project,
                    name = "dumdum",
                    languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST),
                )
                val psiManager = PsiManager.getInstance(project)

                val vFile = virtualFile(fileId) {
                    text.toByteArray()
                }
                val psiFile = psiManager.findFile(vFile)!!

                withAnalyzer(
                    index = index,
                    singleModule = singleModule,
                    virtualFileFactory = { id ->
                        virtualFile(id) {
                            files[id]!!.toByteArray()
                        }
                    }
                ) {
                    analyze(psiFile as KtFile) {
                        psiFile.descendantsOfType<KtCallElement>().forEach { callExpr ->
                            when (val callInfo = callExpr.resolveToCall()) {
                                is KaSuccessCallInfo -> {
                                    when (val call = callInfo.call) {
                                        is KaCallableMemberCall<*, *> -> {
                                            val sym = call.partiallyAppliedSymbol.symbol
                                            println("SUCCESS: $sym ${sym.callableId}")
                                        }
                                        else -> {
                                            println("SUCCESS")
                                        }
                                    }
                                }
                                is KaErrorCallInfo -> {
                                    println("UNRESOLVED: ${callInfo.diagnostic.defaultMessage}")
                                }
                                else -> {
                                    println("wtf")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun main() {
    test1()
}

internal class KaSourceModuleImpl(
    override val directRegularDependencies: List<KaModule>,
    override val directDependsOnDependencies: List<KaModule>,
    override val directFriendDependencies: List<KaModule>,
    override val contentScope: GlobalSearchScope,
    override val targetPlatform: TargetPlatform,
    override val project: Project,
    override val name: String,
    override val languageVersionSettings: LanguageVersionSettings,
    override val transitiveDependsOnDependencies: List<KaModule> = emptyList(),
) : KaSourceModule {
    @KaExperimentalApi
    override val stableModuleName: String? get() = name

    @KaExperimentalApi
    override val psiRoots: List<PsiFileSystemItem>
        get() = throw UnsupportedOperationException("don't do this")
}

internal class KaLibraryModuleImpl(
    override val directRegularDependencies: List<KaModule>,
    override val libraryName: String,
    override val binaryRoots: Collection<Path>,
    @KaExperimentalApi
    override val binaryVirtualFiles: Collection<VirtualFile>,
    override val librarySources: KaLibrarySourceModule?,
    @KaPlatformInterface
    override val isSdk: Boolean,
    override val directDependsOnDependencies: List<KaModule>,
    override val transitiveDependsOnDependencies: List<KaModule>,
    override val directFriendDependencies: List<KaModule>,
    override val contentScope: GlobalSearchScope,
    override val targetPlatform: TargetPlatform,
    override val project: Project,
) : KaLibraryModule

fun createSearchScopeByLibraryRoots(
    binaryRoots: Collection<Path>,
    binaryVirtualFiles: Collection<VirtualFile>,
    project: Project,
    jarFileSystem: VirtualFileSystem,
): GlobalSearchScope {
    @OptIn(KaImplementationDetail::class)
    fun getVirtualFileUrlsForLibraryRootsRecursively(
        binaryVirtualFiles: Collection<VirtualFile>,
    ): Set<String> =
        buildSet {
            for (vf in binaryVirtualFiles) {
                LibraryUtils.getAllVirtualFilesFromRoot(vf, includeRoot = true)
                    .mapTo(this) { it.url }
            }
        }

    @OptIn(KaImplementationDetail::class, KaImplementationDetail::class)
    fun getVirtualFileUrlsForLibraryRootsRecursively(
        roots: Collection<Path>,
        jarFileSystem: VirtualFileSystem,
    ): Set<String> {
        fun getVirtualFilesForLibraryRoots(
            roots: Collection<Path>,
            jarFileSystem: VirtualFileSystem,
        ): List<VirtualFile> {
            fun adjustModulePath(pathString: String): String {
                return if (pathString.contains(URLUtil.JAR_SEPARATOR)) {
                    // URLs loaded from JDK point to module names in a JRT protocol format,
                    // e.g., "jrt:///path/to/jdk/home!/java.base" (JRT protocol prefix + JDK home path + JAR separator + module name)
                    // After protocol erasure, we will see "/path/to/jdk/home!/java.base" as a binary root.
                    // CoreJrtFileSystem.CoreJrtHandler#findFile, which uses Path#resolve, finds a virtual file path to the file itself,
                    // e.g., "/path/to/jdk/home!/modules/java.base". (JDK home path + JAR separator + actual file path)
                    // To work with that JRT handler, a hacky workaround here is to add "modules" before the module name so that it can
                    // find the actual file path.
                    // See [LLFirJavaFacadeForBinaries#getBinaryPath] and [StandaloneProjectFactory#getBinaryPath] for a similar hack.
                    val (libHomePath, pathInImage) = CoreJrtFileSystem.splitPath(pathString)
                    libHomePath + URLUtil.JAR_SEPARATOR + "modules/$pathInImage"
                } else
                    pathString
            }

            return roots.mapNotNull { path ->
                val pathString = FileUtil.toSystemIndependentName(path.toAbsolutePath().toString())
                when {
                    pathString.endsWith(StandardFileSystems.JAR_PROTOCOL) || pathString.endsWith(KLIB_FILE_EXTENSION) -> {
                        jarFileSystem.findFileByPath(pathString + URLUtil.JAR_SEPARATOR)
                    }

                    pathString.contains(URLUtil.JAR_SEPARATOR) -> {
                        jarFileSystem.findFileByPath(adjustModulePath(pathString))
                    }

                    else -> {
                        VirtualFileManager.getInstance().findFileByNioPath(path)
                    }
                }
            }.distinct()
        }

        return buildSet {
            for (root in getVirtualFilesForLibraryRoots(roots, jarFileSystem)) {
                LibraryUtils.getAllVirtualFilesFromRoot(root, includeRoot = true)
                    .mapTo(this) { it.url }
            }
        }
    }

    val virtualFileUrlsFromBinaryRoots = getVirtualFileUrlsForLibraryRootsRecursively(binaryRoots, jarFileSystem)
    val virtualFileUrlsFromBinaryVirtualFiles = getVirtualFileUrlsForLibraryRootsRecursively(binaryVirtualFiles)
    val virtualFileUrls = virtualFileUrlsFromBinaryRoots + virtualFileUrlsFromBinaryVirtualFiles

    return object : GlobalSearchScope(project) {
        override fun contains(file: VirtualFile): Boolean = file.url in virtualFileUrls

        override fun isSearchInModuleContent(aModule: com.intellij.openapi.module.Module): Boolean = false

        override fun isSearchInLibraries(): Boolean = true

        override fun toString(): String = virtualFileUrls.toString()
    }
}
