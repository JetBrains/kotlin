/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services.libraries

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Path

interface TestModuleDecompiler : TestService {
    fun getAllPsiFilesFromLibrary(artifact: Path, project: Project): List<PsiFile>
}

val TestServices.testModuleDecompiler: TestModuleDecompiler by TestServices.testServiceAccessor()

class TestModuleDecompilerJar : TestModuleDecompiler {
    override fun getAllPsiFilesFromLibrary(artifact: Path, project: Project): List<PsiFile> =
        LibraryUtils.getAllPsiFilesFromJar(artifact, project)
}
