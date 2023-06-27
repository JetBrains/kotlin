/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

object LibraryUtils {
    /**
     * Get all [VirtualFile]s inside the given [jar] (of [Path])
     *
     * Note that, if [CoreJarFileSystem] is not given, a fresh instance will be used, which will create fresh instances of [VirtualFile],
     *   resulting in potential hash mismatch (e.g., if used in scope membership check).
     *
     * By default, given [jar], the root, will be included. Pass [includeRoot = false] if not needed.
     *   Note that, thought, [JvmPackagePartProvider#addRoots] is checking if the root file is in the scope when loading Kotlin modules.
     *   Thus, if this util is used to populate files for the scope of the Kotlin module as a library, the root should be added too.
     */
    fun getAllVirtualFilesFromJar(
        jar: Path,
        jarFileSystem: CoreJarFileSystem = CoreJarFileSystem(),
        includeRoot: Boolean = true,
    ): Collection<VirtualFile> {
        return jarFileSystem.refreshAndFindFileByPath(jar.toString() + JAR_SEPARATOR)
            ?.let { getAllVirtualFilesFromRoot(it, includeRoot) } ?: emptySet()
    }

    fun getAllPsiFilesFromJar(
        jar: Path,
        project: Project,
        jarFileSystem: CoreJarFileSystem = CoreJarFileSystem(),
        includeRoot: Boolean = true,
    ): List<PsiFile> {
        val virtualFiles = getAllVirtualFilesFromJar(jar, jarFileSystem, includeRoot)
        return virtualFiles
            .mapNotNull { virtualFile ->
                PsiManager.getInstance(project).findFile(virtualFile)
            }
    }

    /**
     * Get all [VirtualFile]s inside the given [dir] (of [Path])
     *
     * Note that, if [CoreJarFileSystem] is not given, a fresh instance will be used, which will create fresh instances of [VirtualFile],
     *   resulting in potential hash mismatch (e.g., if used in scope membership check).
     */
    fun getAllVirtualFilesFromDirectory(
        dir: Path,
        includeRoot: Boolean = true,
    ): Collection<VirtualFile> {
        val fs = StandardFileSystems.local()
        return fs.findFileByPath(dir.toString())
            ?.let { getAllVirtualFilesFromRoot(it, includeRoot) } ?: emptySet()
    }

    fun getAllVirtualFilesFromRoot(
        root: VirtualFile,
        includeRoot: Boolean,
    ): Collection<VirtualFile> {
        val files = mutableSetOf<VirtualFile>()
        if (includeRoot) {
            files.add(root)
        }
        VfsUtilCore.iterateChildrenRecursively(
            root,
            /*filter=*/{ true },
            /*iterator=*/{ virtualFile ->
                if (!virtualFile.isDirectory) {
                    files.add(virtualFile)
                }
                true
            }
        )
        return files
    }

    // Copied (and adjusted) from JavaSdkImpl#readModulesFromReleaseFile
    private fun readModulesFromReleaseFile(jrtBaseDir: Path): List<String?>? {
        Files.newInputStream(jrtBaseDir.resolve("release")).use { stream ->
            val p = Properties()
            p.load(stream)
            val modules = p.getProperty("MODULES")
            if (modules != null) {
                return StringUtil.split(StringUtil.unquoteString(modules), " ")
            }
        }
        return null
    }

    // Copied from JdkUtil#isModularRuntime
    private fun isModularRuntime(homePath: Path): Boolean {
        return Files.isRegularFile(homePath.resolve("lib/jrt-fs.jar")) || isExplodedModularRuntime(homePath)
    }

    // Copied from JdkUtil#isExplodedModularRuntime
    private fun isExplodedModularRuntime(homePath: Path): Boolean {
        return Files.isDirectory(homePath.resolve("modules/java.base"))
    }

    // Copied (and adjusted) from JavaSdkImpl#findClasses
    // Currently, handle modular runtime only
    fun findClassesFromJdkHome(jdkHome: Path): List<String> {
        val result = mutableListOf<String>()

        if (isModularRuntime(jdkHome)) {
            val jrtBaseUrl = "${StandardFileSystems.JRT_PROTOCOL_PREFIX}${
                FileUtil.toSystemIndependentName(jdkHome.toAbsolutePath().toString())
            }$JAR_SEPARATOR"
            val modules = readModulesFromReleaseFile(jdkHome)
            if (modules != null) {
                for (module in modules) {
                    result.add(jrtBaseUrl + module)
                }
            }
        }

        result.sort()
        return result
    }
}
