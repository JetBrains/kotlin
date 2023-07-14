/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.google.common.io.Files.getFileExtension
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.project.structure.builder.*
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.javaSourceRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.wasm.resolve.WasmJsPlatformAnalyzerServices
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtStaticProjectStructureProvider

internal fun TargetPlatform.getAnalyzerServices(): PlatformDependentAnalyzerServices {
    return when {
        isJvm() -> JvmPlatformAnalyzerServices
        isJs() -> JsPlatformAnalyzerServices
        isWasm() -> WasmJsPlatformAnalyzerServices
        isNative() -> NativePlatformAnalyzerServices
        isCommon() -> CommonPlatformAnalyzerServices
        else -> error("Unknown target platform: $this")
    }
}

/**
 * Collect source file path as [String] from the given source roots in [compilerConfig].
 *
 * Such source roots are either [KotlinSourceRoot] or [JavaSourceRoot], and thus
 * this util collects all `.kt` and `.java` files under source roots.
 */
internal fun getSourceFilePaths(
    compilerConfig: CompilerConfiguration,
    includeDirectoryRoot: Boolean = false,
): Set<String> {
    return buildSet {
        compilerConfig.javaSourceRoots.forEach { srcRoot ->
            val path = Paths.get(srcRoot)
            if (Files.isDirectory(path)) {
                // E.g., project/app/src
                collectSourceFilePaths(path, this)
                if (includeDirectoryRoot) {
                    add(srcRoot)
                }
            } else {
                // E.g., project/app/src/some/pkg/main.kt
                add(srcRoot)
            }
        }
    }
}

/**
 * Collect source file path from the given [root] store them in [result].
 *
 * E.g., for `project/app/src` as a [root], this will walk the file tree and
 * collect all `.kt`, `.kts`, and `.java` files under that folder.
 *
 * Note that this util gracefully skips [IOException] during file tree traversal.
 */
private fun collectSourceFilePaths(
    root: Path,
    result: MutableSet<String>
) {
    // NB: [Files#walk] throws an exception if there is an issue during IO.
    // With [Files#walkFileTree] with a custom visitor, we can take control of exception handling.
    Files.walkFileTree(
        root,
        object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                return if (Files.isReadable(dir))
                    FileVisitResult.CONTINUE
                else
                    FileVisitResult.SKIP_SUBTREE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (!Files.isRegularFile(file) || !Files.isReadable(file))
                    return FileVisitResult.CONTINUE
                val ext = getFileExtension(file.fileName.toString())
                if (ext == KotlinFileType.EXTENSION ||
                    ext == KotlinParserDefinition.STD_SCRIPT_SUFFIX ||
                    ext == JavaFileType.DEFAULT_EXTENSION
                ) {
                    result.add(file.toString())
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException?): FileVisitResult {
                // TODO: report or log [IOException]?
                // NB: this intentionally swallows the exception, hence fail-safe.
                // Skipping subtree doesn't make any sense, since this is not a directory.
                // Skipping sibling may drop valid file paths afterward, so we just continue.
                return FileVisitResult.CONTINUE
            }
        }
    )
}

internal inline fun <reified T : PsiFileSystemItem> getPsiFilesFromPaths(
    project: Project,
    paths: Collection<String>,
): List<T> {
    val fs = StandardFileSystems.local()
    val psiManager = PsiManager.getInstance(project)
    return buildList {
        for (path in paths) {
            val vFile = fs.findFileByPath(path) ?: continue
            val psiFileSystemItem =
                if (vFile.isDirectory)
                    psiManager.findDirectory(vFile) as? T
                else
                    psiManager.findFile(vFile) as? T
            psiFileSystemItem?.let { add(it) }
        }
    }
}

internal fun buildKtModuleProviderByCompilerConfiguration(
    compilerConfig: CompilerConfiguration,
    project: Project,
    ktFiles: List<KtFile>,
): KtStaticProjectStructureProvider = buildProjectStructureProvider {
    val (scriptFiles, ordinaryFiles) = ktFiles.partition { it.isScript() }
    val platform = JvmPlatforms.defaultJvmPlatform

    fun KtModuleBuilder.addModuleDependencies(moduleName: String) {
        val libraryRoots = compilerConfig.jvmModularRoots + compilerConfig.jvmClasspathRoots
        addRegularDependency(
            buildKtLibraryModule {
                contentScope = ProjectScope.getLibrariesScope(project)
                this.platform = platform
                this.project = project
                binaryRoots = libraryRoots.map { it.toPath() }
                libraryName = "Library for $moduleName"
            }
        )
        compilerConfig.get(JVMConfigurationKeys.JDK_HOME)?.let { jdkHome ->
            val vfm = VirtualFileManager.getInstance()
            val jdkHomePath = jdkHome.toPath()
            val jdkHomeVirtualFile = vfm.findFileByNioPath(jdkHomePath)
            val binaryRoots = LibraryUtils.findClassesFromJdkHome(jdkHomePath).map {
                Paths.get(URLUtil.extractPath(it))
            }
            addRegularDependency(
                buildKtSdkModule {
                    contentScope = GlobalSearchScope.fileScope(project, jdkHomeVirtualFile)
                    this.platform = platform
                    this.project = project
                    this.binaryRoots = binaryRoots
                    sdkName = "JDK for $moduleName"
                }
            )
        }
    }

    val configLanguageVersionSettings = compilerConfig[CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS]

    for (scriptFile in scriptFiles) {
        buildKtScriptModule {
            configLanguageVersionSettings?.let { this.languageVersionSettings = it }
            this.project = project
            this.platform = platform
            this.file = scriptFile

            addModuleDependencies("Script " + scriptFile.name)
        }.apply(::addModule)
    }

    buildKtSourceModule {
        configLanguageVersionSettings?.let { this.languageVersionSettings = it }
        this.project = project
        this.platform = platform
        this.moduleName = compilerConfig.get(CommonConfigurationKeys.MODULE_NAME) ?: "<no module name provided>"

        addModuleDependencies(moduleName)

        contentScope = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ordinaryFiles)
        addSourceRoots(
            getPsiFilesFromPaths(
                project,
                getSourceFilePaths(compilerConfig, includeDirectoryRoot = true)
            )
        )
    }.apply(::addModule)


    this.platform = platform
    this.project = project
}
