@file:OptIn(KaPlatformInterface::class, KaExperimentalApi::class)

package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.*
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.descendantsOfType
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.dumdum.index.FileId
import org.jetbrains.kotlin.analysis.api.dumdum.index.FileValues
import org.jetbrains.kotlin.analysis.api.dumdum.index.compose
import org.jetbrains.kotlin.analysis.api.dumdum.index.inMemoryIndex
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaErrorCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaSuccessCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.nio.file.Path

data class StringFileId(val id: String) : FileId {
    override val fileName: String get() = id
}

data class JarFileId(val id: String) : FileId {
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
                    
                    """.trimIndent().toByteArray(),

            StringFileId("/src/bar/bar.kt") to """

                    package bar
                    
                    import foo.Foo
                    import foo.foo 
                    
                    class Bar {
                        fun bar() { Foo().foo() }
                    }
                    
                    fun bar() { foo() }
                    
                    """.trimIndent().toByteArray(),

            StringFileId("/src/bar/baz.kt") to """

                    package baz
                    
                    fun baz() { listOf(1) }
                    
                    """.trimIndent().toByteArray(),

            StringFileId("/src/bar/fizz.kt") to """

                    package fizz
                    
                    fun Int.extension() {}
                    
                    fun Float.extension() {}
                    
                    fun fizz() {
                        1.extension()
                        1f.extension()
                    }
                    
                    """.trimIndent().toByteArray()
        )

        val stdlibPath = File("/Users/jetzajac/Projects/kotlin/dist/kotlinc/lib/kotlin-stdlib.jar").toPath()
        val jarFiles =
            buildMap {
                val jarRoot = jarFileSystem.findFileByPath("$stdlibPath!/")!!
                VfsUtilCore.visitChildrenRecursively(jarRoot, object : VirtualFileVisitor<Any>() {
                    override fun visitFile(file: VirtualFile): Boolean {
                        if (!file.isDirectory) {
                            put(JarFileId(file.path), file)
                        }
                        return true
                    }
                })

            }

        val libIndex = withProject {
            val psiManager = PsiManager.getInstance(project)
            val jarFileValues: Map<FileId, FileValues> = jarFiles.mapNotNull { (jarFileId, virtualFile) ->
                psiManager.findFile(virtualFile)?.let { psiFile ->
                    jarFileId to mapFile(psiFile)
                } ?: run {
                    println("PSI File not found for jar file: $virtualFile")
                    null
                }
            }.toMap()
            inMemoryIndex(jarFileValues)
        }

        val sourceIndex = withProject {
            val psiManager = PsiManager.getInstance(project)
            val sourceFileValues: Map<FileId, FileValues> = files.mapNotNull { (fileId, fileBytes) ->
                val vFile = virtualFile(fileId) {
                    fileBytes
                }
                psiManager.findFile(vFile)?.let { psiFile ->
                    fileId to mapFile(psiFile)
                } ?: run {
                    println("PSI File not found for source file: $vFile")
                    null
                }
            }.toMap()
            inMemoryIndex(
                sourceFileValues
            )
        }

        val index = libIndex.compose(sourceIndex)

        files.forEach { (fileId, fileBytes) ->
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
                    contentScope = GlobalSearchScope.filesScope(project, jarFiles.values),
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
                    fileBytes
                }
                val psiFile = psiManager.findFile(vFile)!!

                withAnalyzer(
                    index = index,
                    singleModule = singleModule,
                    virtualFileFactory = { id ->
                        when (id) {
                            is JarFileId -> jarFiles[id]!!
                            is StringFileId -> virtualFile(id) {
                                files[id]!!
                            }
                            else -> error("unknown file id")
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

