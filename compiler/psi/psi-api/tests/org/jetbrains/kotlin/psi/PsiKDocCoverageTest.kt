/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.AbstractKDocCoverageTest
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Test

class PsiKDocCoverageTest : AbstractKDocCoverageTest() {
    override val sourceDirectories: List<SourceDirectory.ForDumpFileComparison> = listOf(
        SourceDirectory.ForDumpFileComparison(
            listOf(
                "src/org/jetbrains/kotlin",
                "../psi-impl/src/org/jetbrains/kotlin",
                "../psi-utils/src/org/jetbrains/kotlin",
                "../psi-frontend-utils/src/org/jetbrains/kotlin",
            ),
            "api/psi-api.undocumented",
        )
    )

    override val ignoredPackages: List<FqName> = listOf(
        FqName("org.jetbrains.kotlin.idea"),
        FqName("org.jetbrains.kotlin.lexer"),
        FqName("org.jetbrains.kotlin.parsing"),
        FqName("org.jetbrains.kotlin.kdoc.lexer"),
        FqName("org.jetbrains.kotlin.kdoc.parser"),
    )

    @Test
    fun testKDocCoverage() {
        doTest()
    }
}