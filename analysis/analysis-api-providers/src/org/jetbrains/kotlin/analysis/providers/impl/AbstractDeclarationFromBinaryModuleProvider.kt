/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.analysis.project.structure.KtBinaryModule
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.FqName
import java.nio.file.Files
import java.nio.file.Path

public interface AbstractDeclarationFromBinaryModuleProvider {
    public val scope: GlobalSearchScope
    public val packagePartProvider: PackagePartProvider
    public val jarFileSystem: CoreJarFileSystem

    /**
     * Collect [VirtualFile]s that belong to the package with the given [FqName],
     * from the given [KtBinaryModule], which is supposed to be a Kotlin module (i.e., with `kotlin_module` info),
     * and properly registered to [PackagePartProvider]. Otherwise, returns an empty set.
     *
     * This util is useful to collect files for the package that may have multi-file facades.
     * E.g., for `kotlin.collection`, regular classes would be under `kotlin/collection` folder.
     * But, there could be more classes under irregular places, like `.../jdk8/...`,
     * which would still have `kotlin.collection` as a package, if it is part of multi-file facades.
     *
     * To cover such cases with a normal, exhaustive directory lookup used in [virtualFilesFromModule], we will end up
     * traversing _all_ folders, which is inefficient if package part information is available in `kotlin_module`.
     */
    public fun virtualFilesFromKotlinModule(
        binaryModule: KtBinaryModule,
        fqName: FqName,
    ): Set<VirtualFile> {
        val fqNameString = fqName.asString()
        val packageParts = packagePartProvider.findPackageParts(fqNameString)
        return if (packageParts.isNotEmpty()) {
            binaryModule.getBinaryRoots().flatMap r@{ rootPath ->
                if (!Files.isRegularFile(rootPath) || ".jar" !in rootPath.toString()) return@r emptySet<VirtualFile>()
                buildSet {
                    packageParts.forEach { packagePart ->
                        add(
                            jarFileSystem.refreshAndFindFileByPath(
                                rootPath.toAbsolutePath().toString() + URLUtil.JAR_SEPARATOR + packagePart + ".class"
                            ) ?: return@r emptySet<VirtualFile>()
                        )
                    }
                }
            }.toSet()
        } else
            emptySet()
    }

    /**
     * Collect [VirtualFile]s that belong to the package with the given [FqName],
     * from the given [KtBinaryModule], which has general `jar` files as roots, e.g., `android.jar` (for a specific API version)
     *
     * If the given [FqName] is a specific class name, returns a set with the corresponding [VirtualFile].
     *
     * This util assumes that classes will be under the folder where the folder path and package name match.
     * To avoid exhaustive traversal, this util only visits folders that are parts of the given package name.
     * E.g., for `android.os`, this will visit `android` and `android/os` directories only,
     * and will return [VirtualFile]s for all classes under `android/os`.
     *
     * For a query with a class name, e.g., `android.os.Bundle`, this will visit `android` and `android/os` directories too,
     * to search for that specific class.
     */
    public fun virtualFilesFromModule(
        binaryModule: KtBinaryModule,
        fqName: FqName,
        isPackageName: Boolean,
    ): Set<VirtualFile> {
        val fqNameString = fqName.asString()
        val fs = StandardFileSystems.local()
        return binaryModule.getBinaryRoots().flatMap r@{ rootPath ->
            val root = findRoot(rootPath, fs) ?: return@r emptySet()
            val files = mutableSetOf<VirtualFile>()
            VfsUtilCore.iterateChildrenRecursively(
                root,
                /*filter=*/filter@{
                    // Return `false` will skip the children.
                    if (it == root) return@filter true
                    // If it is a directory, then check if its path starts with fq name of interest
                    val relativeFqName = relativeFqName(root, it)
                    if (it.isDirectory && fqNameString.startsWith(relativeFqName)) {
                        return@filter true
                    }
                    // Otherwise, i.e., if it is a file, we are already in that matched directory (or directory in the middle).
                    // But, for files at the top-level, double-check if its parent (dir) and fq name of interest match.
                    if (isPackageName)
                        relativeFqName(root, it.parent).endsWith(fqNameString)
                    else // exact class fq name
                        relativeFqName == fqNameString
                },
                /*iterator=*/{
                    // We reach here after filtering above.
                    // Directories in the middle, e.g., com/android, can reach too.
                    if (!it.isDirectory &&
                        isCompiledFile(it) &&
                        it in scope
                    ) {
                        files.add(it)
                    }
                    true
                }
            )
            files
        }.toSet()
    }

    private fun findRoot(
        rootPath: Path,
        fs: VirtualFileSystem,
    ): VirtualFile? {
        return if (Files.isRegularFile(rootPath) && ".jar" in rootPath.toString()) {
            jarFileSystem.refreshAndFindFileByPath(rootPath.toAbsolutePath().toString() + URLUtil.JAR_SEPARATOR)
        } else {
            fs.findFileByPath(rootPath.toAbsolutePath().toString())
        }
    }

    private fun relativeFqName(
        root: VirtualFile,
        virtualFile: VirtualFile,
    ): String {
        return if (root.isDirectory) {
            val fragments = buildList {
                var cur = virtualFile
                while (cur != root) {
                    add(cur.nameWithoutExtension)
                    cur = cur.parent
                }
            }
            fragments.reversed().joinToString(".")
        } else {
            virtualFile.path.split(URLUtil.JAR_SEPARATOR).lastOrNull()?.replace("/", ".")
                ?: URLUtil.JAR_SEPARATOR // random string that will bother membership test.
        }
    }

    private fun isCompiledFile(
        virtualFile: VirtualFile,
    ): Boolean {
        return virtualFile.extension?.endsWith(JavaClassFileType.INSTANCE.defaultExtension) == true
    }
}
