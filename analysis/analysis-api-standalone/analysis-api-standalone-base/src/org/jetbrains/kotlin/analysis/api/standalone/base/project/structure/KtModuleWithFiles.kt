/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.project.structure

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneProjectFactory.findJvmRootsForJavaFiles
import org.jetbrains.kotlin.analysis.project.structure.KtBinaryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule

data class KtModuleProjectStructure(
    val mainModules: List<KtModuleWithFiles>,
    val binaryModules: Iterable<KtBinaryModule>,
) {
    fun allKtModules(): List<KtModule> = buildList {
        mainModules.mapTo(this) { it.ktModule }
        addAll(binaryModules)
    }

    fun allSourceFiles(): List<PsiFileSystemItem> = buildList {
        val files = mainModules.flatMap { it.files }
        addAll(files)
        addAll(findJvmRootsForJavaFiles(files.filterIsInstance<PsiJavaFile>()))
    }
}

data class KtModuleWithFiles(
    val ktModule: KtModule,
    val files: List<PsiFile>
)