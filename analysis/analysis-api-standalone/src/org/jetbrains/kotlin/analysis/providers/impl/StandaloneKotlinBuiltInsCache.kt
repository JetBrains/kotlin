/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiManager
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.FileContentImpl
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.analysis.providers.KotlinBuiltInsCache
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

/**
 * A cache to store loaded built-ins of raw [FileContent] and reuse them as long as this application-level service is available.
 *
 * Loading fully decompiled built-ins requires [Project]-level service, [PsiManager].
 * Therefore, only [KotlinStaticDeclarationProvider] will trigger loading built-ins with that service.
 */
internal class StandaloneKotlinBuiltInsCache : KotlinBuiltInsCache() {
    private val cache: Collection<FileContent> by lazy {
        val jarFileSystem = CoreJarFileSystem()
        val classLoader = this::class.java.classLoader
        buildList {
            StandardClassIds.builtInsPackages.forEach { builtInPackageFqName ->
                val resourcePath = BuiltInSerializerProtocol.getBuiltInsFilePath(builtInPackageFqName)
                classLoader.getResource(resourcePath)?.let { resourceUrl ->
                    // "file:///path/to/stdlib.jar!/builtin/package/.kotlin_builtins
                    //   -> ("path/to/stdlib.jar", "builtin/package/.kotlin_builtins")
                    URLUtil.splitJarUrl(resourceUrl.path)?.let {
                        val jarPath = it.first
                        val builtInFile = it.second
                        val pathToQuery = jarPath + URLUtil.JAR_SEPARATOR + builtInFile
                        jarFileSystem.findFileByPath(pathToQuery)?.let { vf ->
                            add(FileContentImpl.createByFile(vf))
                        }
                    }
                }
            }
        }
    }

    override fun getOrLoadBuiltIns(
        project: Project,
    ): Collection<KtDecompiledFile> {
        val psiManager = PsiManager.getInstance(project)
        return cache.mapNotNull { createKtFileStub(psiManager, it) }
    }

    private fun createKtFileStub(
        psiManager: PsiManager,
        fileContent: FileContent,
    ): KtDecompiledFile? {
        val builtInDecompiler = KotlinBuiltInDecompiler()
        val fileViewProvider =
            builtInDecompiler.createFileViewProvider(fileContent.file, psiManager, physical = true) as? KotlinDecompiledFileViewProvider
                ?: return null
        return builtInDecompiler.readFile(fileContent.content, fileContent.file)?.let { fileWithMetadata ->
            KtDecompiledFile(fileViewProvider) {
                builtInDecompiler.buildDecompiledText(fileWithMetadata)
            }
        }
    }
}