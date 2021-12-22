/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.decompiled.service

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.TestKtLibraryModule
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.projectModuleProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.registerTestServices
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.writeText


class LibraryModuleRegistrarPreAnalysisHandler(
    testServices: TestServices
) : PreAnalysisHandler(testServices) {
    private val moduleInfoProvider = testServices.projectModuleProvider

    override fun preprocessModuleStructure(moduleStructure: TestModuleStructure) {
        val testModule = moduleStructure.modules.single()
        val project = testServices.compilerConfigurationProvider.getProject(testModule)
        val decompiledKtFiles = getDecompiledVirtualFilesFromLibrary(testModule, project)

        moduleInfoProvider.registerModuleInfo(testModule, TestKtLibraryModule(project, testModule, decompiledKtFiles, testServices))
        (project as MockProject).registerTestServices(testModule, decompiledKtFiles, testServices)
    }

    private fun getDecompiledVirtualFilesFromLibrary(module: TestModule, project: Project): List<KtClsFile> {
        val tmpDir = KtTestUtil.tmpDir("testSourcesToCompile").toPath()
        for (testFile in module.files) {
            val text = testServices.sourceFileProvider.getContentOfSourceFile(testFile)
            val tmpSourceFile = (tmpDir / testFile.name).createFile()
            tmpSourceFile.writeText(text)
        }
        val library = CompilerExecutor.compileLibrary(
            tmpDir,
            CompilerExecutor.parseCompilerOptionsFromTestdata(module),
            compilationErrorExpected = CompilerExecutor.Directives.COMPILATION_ERRORS in module.directives
        ) ?: return emptyList()

        val virtualFiles = getAllVirtualFilesFromLibrary(library)
        return virtualFiles.mapNotNull { virtualFile ->
            PsiManager.getInstance(project).findFile(virtualFile) as? KtClsFile
        }
    }


    private fun getAllVirtualFilesFromLibrary(library: Path): Collection<VirtualFile> {
        val jarFileSystem = CoreJarFileSystem()
        val root = jarFileSystem.refreshAndFindFileByPath(library.absolutePathString() + "!/")!!

        val files = mutableSetOf<VirtualFile>()
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
}
