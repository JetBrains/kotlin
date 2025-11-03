/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.util

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.io.URLUtil
import com.intellij.util.io.URLUtil.JAR_PROTOCOL
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils.getVirtualFilesForLibraryRoots
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile

@KaImplementationDetail
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
        return virtualFiles.mapToPsiFiles(project)
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

    fun getAllPsiFilesFromDirectory(
        dir: Path,
        project: Project,
        includeRoot: Boolean = true,
    ): List<PsiFile> {
        return getAllVirtualFilesFromDirectory(dir, includeRoot).mapToPsiFiles(project)
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
                files.add(virtualFile)
                true
            }
        )
        return files
    }

    fun findClassesFromJdkHome(jdkHome: Path, isJre: Boolean): List<Path> {
        return JdkClassFinder.findClasses(jdkHome, isJre).map { rawPath ->
            val path = URLUtil.extractPath(rawPath).removeSuffix("/").removeSuffix("!")
            Paths.get(path)
        }
    }

    private fun Collection<VirtualFile>.mapToPsiFiles(project: Project): List<PsiFile> {
        return mapNotNull { virtualFile ->
            PsiManager.getInstance(project).findFile(virtualFile)
        }
    }

    /**
     * Returns [VirtualFile]s representing [roots] in their corresponding file system, e.g., local or JAR file system.
     */
    fun getVirtualFilesForLibraryRoots(
        roots: Collection<Path>,
        environment: CoreApplicationEnvironment?,
    ): List<VirtualFile> {
        return roots.mapNotNull { path ->
            val pathString = FileUtil.toSystemIndependentName(path.toAbsolutePath().toString())
            val isRegularFile = path.isRegularFile()
            when {
                isRegularFile && (pathString.endsWith(JAR_PROTOCOL) || pathString.endsWith(KLIB_FILE_EXTENSION)) -> {
                    val fileSystem =
                        environment?.jarFileSystem ?: VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.JAR_PROTOCOL)

                    fileSystem.findFileByPath(pathString + JAR_SEPARATOR)
                }

                pathString.contains(JAR_SEPARATOR) -> {
                    val fileSystem =
                        environment?.jrtFileSystem ?: VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.JRT_PROTOCOL)

                    fileSystem.findFileByPath(adjustModulePath(pathString))
                }
                else -> {
                    VirtualFileManager.getInstance().findFileByNioPath(path)
                }
            }
        }.distinct()
    }

    /**
     * A counterpart for [getVirtualFilesForLibraryRoots].
     *
     * This function is needed because library virtual files can be of two types:
     * - Virtual file from the JAR file system
     * - Virtual file from the local file system representing the JAR file itself
     *
     * Since we need [Path] from the local file system,
     * all [VirtualFile]s from the JAR file system should be mapped to their corresponding JAR file.
     */
    fun getLibraryPathsForVirtualFiles(
        virtualFiles: Collection<VirtualFile>,
    ): List<Path> {
        return virtualFiles.mapNotNull { file ->
            val pathString = FileUtil.toSystemIndependentName(file.path)
            when {
                pathString.endsWith(".$JAR_PROTOCOL$JAR_SEPARATOR") || pathString.endsWith(".$KLIB_FILE_EXTENSION$JAR_SEPARATOR") ->
                    Paths.get(pathString.substringBefore(JAR_SEPARATOR))
                "${JAR_SEPARATOR}modules/" in pathString -> {
                    /**
                     * CoreJrtFileSystem.CoreJrtHandler#findFile, which uses Path#resolve, finds a virtual file path to the file itself,
                     * e.g., "/path/to/jdk/home!/modules/java.base/java/lang/Object.class". (JDK home path + JAR separator + actual file path)
                     * URLs loaded from JDK, though, point to module names in a JRT protocol format,
                     * e.g., "jrt:///path/to/jdk/home!/java.base" (JRT protocol prefix + JDK home path + JAR separator + module name)
                     * After splitting at the JAR separator, it is regarded as a root directory "/java.base".
                     * A hacky workaround here is to remove "modules/" from actual file path.
                     * e.g. "/path/to/jdk/home!/java.base/java/lang/Object.class", which, from Path viewpoint, belongs to "/java.base",
                     * after splitting at the JAR separator, in a similar way.
                     */
                    Paths.get(pathString.replace("${JAR_SEPARATOR}modules/", JAR_SEPARATOR))
                }
                else ->
                    Paths.get(pathString)
            }
        }.distinct()
    }

    private fun adjustModulePath(pathString: String): String {
        return if (pathString.contains(JAR_SEPARATOR)) {
            /** URLs loaded from JDK point to module names in a JRT protocol format,
             * e.g., "jrt:///path/to/jdk/home!/java.base" (JRT protocol prefix + JDK home path + JAR separator + module name)
             * After protocol erasure, we will see "/path/to/jdk/home!/java.base" as a binary root.
             * CoreJrtFileSystem.CoreJrtHandler#findFile, which uses Path#resolve, finds a virtual file path to the file itself,
             * e.g., "/path/to/jdk/home!/modules/java.base". (JDK home path + JAR separator + actual file path)
             * To work with that JRT handler, a hacky workaround here is to add "modules" before the module name so that it can
             * find the actual file path.
             */
            val (libHomePath, pathInImage) = CoreJrtFileSystem.splitPath(pathString)
            libHomePath + JAR_SEPARATOR + "modules/$pathInImage"
        } else
            pathString
    }
}
