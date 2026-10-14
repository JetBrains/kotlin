/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

/**
 * Check that every Kotlin file under `src` in the given module has a correct
 * package name that corresponds to the real file path.
 *
 * If any violations are found, they are reported through an exception.
 */
abstract class AbstractPackageNameTest : AbstractAnalysisApiCodebaseValidationTest() {
    private val moduleSourceRoot: File = File(KtTestUtil.getHomeDirectory(), "src")

    override val sourceDirectories = listOf(
        SourceDirectory.ForValidation(
            sourcePaths = listOf("src"),
        )
    )

    override fun processFile(file: File, psiFile: PsiFile) {
        if (psiFile !is KtFile) {
            return
        }

        require(file.startsWith(moduleSourceRoot))

        val relativeFilePath = file.relativeTo(moduleSourceRoot)
        val relativeInvariantFilePath = relativeFilePath.invariantSeparatorsPath
        if (relativeInvariantFilePath in ignoredFiles) {
            return
        }

        val expectedPackage = getExpectedPackageFqName(relativeFilePath)
        val actualPackage = psiFile.packageFqName
        if (expectedPackage != actualPackage) {
            error("$relativeInvariantFilePath has inconsistent package name: $actualPackage, expected: $expectedPackage")
        }
    }

    /**
     * Files that should be excluded from the validation.
     * Each entry is a path to the file from the module `src` directory, e.g.,
     * `org/jetbrains/kotlin/analysis/api/something/myFile.kt`.
     *
     * Should ONLY be used for some obsolete / non-API files that are in the maintenance mode.
     * Do not use it to shadow real problems.
     * See KT-89733
     */
    protected open val ignoredFiles: Set<String> = emptySet()

    private fun getExpectedPackageFqName(relativeFilePath: File): FqName {
        val relativePackagePath = relativeFilePath.parentFile?.invariantSeparatorsPath.orEmpty()
        return FqName(relativePackagePath.replace('/', '.'))
    }
}

