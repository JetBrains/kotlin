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
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.name.FqName
import java.nio.file.Path

public interface AbstractDeclarationFromLibraryModuleProvider {
    public val scope: GlobalSearchScope
    public val jarFileSystem: CoreJarFileSystem

    public fun virtualFilesFromModule(
        libraryModule: KtLibraryModule,
        fqName: FqName,
        isPackageName: Boolean,
    ): Collection<VirtualFile> {
        val fqNameString = fqName.asString()
        val fs = StandardFileSystems.local()
        return libraryModule.getBinaryRoots().flatMap r@{ rootPath ->
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
        }
    }

    private fun findRoot(
        rootPath: Path,
        fs: VirtualFileSystem,
    ): VirtualFile? {
        return if (rootPath.toFile().isDirectory) {
            fs.findFileByPath(rootPath.toAbsolutePath().toString())
        } else {
            jarFileSystem.refreshAndFindFileByPath(rootPath.toAbsolutePath().toString() + URLUtil.JAR_SEPARATOR)
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
