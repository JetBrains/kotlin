/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProviderByKtModule
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Path

public class KotlinStaticPackageProviderByKtModule(
    project: Project,
    moduleToFiles: List<Pair<KtModule, List<PsiFile>>>,
    binaryModules: List<KtBinaryModule>,
) : KotlinPackageProviderByKtModule() {
    private val moduleToFqNames = buildMap {
        collectSourcePackageMapping(moduleToFiles)
        collectBinaryPackageMapping(binaryModules, project)
    }

    private fun MutableMap<KtModule, Collection<FqName>>.collectBinaryPackageMapping(
        binaryModules: List<KtBinaryModule>,
        project: Project
    ) {
        val jarFileSystem = CoreJarFileSystem()
        val psiManager = PsiManager.getInstance(project)

        for (binaryModule in binaryModules) {
            val packages = binaryModule.getBinaryRoots()
                .mapNotNull { path -> getVirtualFile(path, jarFileSystem) }
                .flatMap(::collectVirtualFilesRecursively)
                .map(psiManager::findFile)
                .filterIsInstance<KtFile>()
                .map { it.packageFqName }

            put(binaryModule, packages)
        }
    }

    private fun getVirtualFile(path: Path, jarFileSystem: CoreJarFileSystem): VirtualFile? {
        val pathString = path.toAbsolutePath().toString()
        return when {
            pathString.contains(URLUtil.JAR_PROTOCOL) -> jarFileSystem.findFileByPath(pathString + URLUtil.JAR_SEPARATOR)
            else -> VirtualFileManager.getInstance().findFileByNioPath(path)
        }
    }

    private fun collectVirtualFilesRecursively(root: VirtualFile): List<VirtualFile> {
        return buildList {
            VfsUtilCore.iterateChildrenRecursively(
                root,
                /*filter=*/{ true },
                /*iterator=*/{ virtualFile ->
                    if (!virtualFile.isDirectory) {
                        add(virtualFile)
                    }
                    true
                }
            )
        }
    }

    private fun MutableMap<KtModule, Collection<FqName>>.collectSourcePackageMapping(
        moduleToFiles: List<Pair<KtModule, List<PsiFile>>>
    ) {
        for ((ktModule, files) in moduleToFiles) {
            val packages = files.filterIsInstance<KtFile>().map { it.packageFqName }
            put(ktModule, packages)
        }
    }

    override fun getContainedPackages(ktModule: KtModule): Collection<FqName> {
        return when (ktModule) {
            is KtBuiltinsModule -> StandardNames.BUILT_INS_PACKAGE_FQ_NAMES
            is KtLibrarySourceModule -> getContainedPackages(ktModule.binaryLibrary)
            is KtLibraryModule -> moduleToFqNames[ktModule].orEmpty()
            is KtSdkModule -> moduleToFqNames[ktModule].orEmpty()
            is KtNotUnderContentRootModule -> moduleToFqNames[ktModule].orEmpty()
            is KtSourceModule -> moduleToFqNames[ktModule].orEmpty()
        }
    }
}