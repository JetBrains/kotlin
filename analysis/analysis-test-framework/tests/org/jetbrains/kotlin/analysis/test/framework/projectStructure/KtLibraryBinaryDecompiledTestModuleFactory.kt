/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.testModuleDecompiler
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Path

/**
 * @see TestModuleKind.LibraryBinaryDecompiled
 */
object KtLibraryBinaryDecompiledTestModuleFactory : KtLibraryBinaryTestModuleFactoryBase() {
    override val testModuleKind: TestModuleKind
        get() = TestModuleKind.LibraryBinaryDecompiled

    override fun decompileToPsiFiles(binaryRoot: Path, testServices: TestServices, project: Project): List<PsiFile> =
        testServices.testModuleDecompiler.getAllPsiFilesFromLibrary(binaryRoot, project)
}
