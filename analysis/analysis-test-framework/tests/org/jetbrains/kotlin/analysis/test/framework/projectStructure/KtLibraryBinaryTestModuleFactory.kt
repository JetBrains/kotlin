/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Path

/**
 * @see TestModuleKind.LibraryBinary
 */
object KtLibraryBinaryTestModuleFactory : KtLibraryBinaryTestModuleFactoryBase() {
    override val testModuleKind: TestModuleKind
        get() = TestModuleKind.LibraryBinary

    override fun decompileToPsiFiles(binaryRoot: Path, testServices: TestServices, project: Project): List<PsiFile> = emptyList()
}
