/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtStaticProjectStructureProvider
import org.jetbrains.kotlin.analysis.project.structure.builder.*
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
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
import kotlin.io.path.extension

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
): Set<Path> {
    return buildSet {
        compilerConfig.javaSourceRoots.forEach { srcRoot ->
            val path = Paths.get(srcRoot)
            if (Files.isDirectory(path)) {
                // E.g., project/app/src
                addAll(collectSourceFilePaths(path))
                if (includeDirectoryRoot) {
                    add(path)
                }
            } else {
                // E.g., project/app/src/some/pkg/main.kt
                add(path)
            }
        }
    }
}

/**
 * Collect source file path from the given [root]
 *
 * E.g., for `project/app/src` as a [root], this will walk the file tree and
 * collect all `.kt`, `.kts`, and `.java` files under that folder.
 *
 * Note that this util gracefully skips [IOException] during file tree traversal.
 */
internal fun collectSourceFilePaths(root: Path): List<Path> {
    // NB: [Files#walk] throws an exception if there is an issue during IO.
    // With [Files#walkFileTree] with a custom visitor, we can take control of exception handling.
    val result = mutableListOf<Path>()
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
                if (file.hasSuitableExtensionToAnalyse()) {
                    result.add(file)
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
    return result
}

internal fun Path.hasSuitableExtensionToAnalyse(): Boolean {
    val extension = extension

    return extension == KotlinFileType.EXTENSION ||
            extension == KotlinParserDefinition.STD_SCRIPT_SUFFIX ||
            extension == JavaFileType.DEFAULT_EXTENSION
}

internal inline fun <reified T : PsiFileSystemItem> getPsiFilesFromPaths(
    kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
    paths: Collection<Path>,
): List<T> {
    val fs = kotlinCoreProjectEnvironment.environment.localFileSystem
    val psiManager = PsiManager.getInstance(kotlinCoreProjectEnvironment.project)
    return buildList {
        for (path in paths) {
            val vFile = fs.findFileByPath(path.toString()) ?: continue
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
    kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
    compilerConfig: CompilerConfiguration,
    ktFiles: List<KtFile>,
): KtStaticProjectStructureProvider = buildProjectStructureProvider(kotlinCoreProjectEnvironment) {
    val (scriptFiles, _) = ktFiles.partition { it.isScript() }
    val platform = JvmPlatforms.defaultJvmPlatform

    fun KtModuleBuilder.addModuleDependencies(moduleName: String) {
        val libraryRoots = compilerConfig.jvmModularRoots + compilerConfig.jvmClasspathRoots
        addRegularDependency(
            buildKtLibraryModule {
                this.platform = platform
                addBinaryRoots(libraryRoots.map { it.toPath() })
                libraryName = "Library for $moduleName"
            }
        )
        compilerConfig.get(JVMConfigurationKeys.JDK_HOME)?.let { jdkHome ->
            addRegularDependency(
                buildKtSdkModule {
                    this.platform = platform
                    addBinaryRootsFromJdkHome(jdkHome.toPath(), isJre = false)
                    sdkName = "JDK for $moduleName"
                }
            )
        }
    }

    val configLanguageVersionSettings = compilerConfig[CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS]

    for (scriptFile in scriptFiles) {
        buildKtScriptModule {
            configLanguageVersionSettings?.let { this.languageVersionSettings = it }
            this.platform = platform
            this.file = scriptFile

            addModuleDependencies("Script " + scriptFile.name)
        }.apply(::addModule)
    }

    buildKtSourceModule {
        configLanguageVersionSettings?.let { this.languageVersionSettings = it }
        this.platform = platform
        this.moduleName = compilerConfig.get(CommonConfigurationKeys.MODULE_NAME) ?: "<no module name provided>"

        addModuleDependencies(moduleName)

        addSourceRoots(compilerConfig.javaSourceRoots.map { Paths.get(it) })
    }.apply(::addModule)


    this.platform = platform
}
