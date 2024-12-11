/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.KDocCoverageTest

class PsiKDocCoverageTest() : KDocCoverageTest() {
    override val sourceCodePath: String = "/compiler/psi/src/org/jetbrains/kotlin"
    override val generatedFilePath: String =
        "/compiler/testData/psiKdocCoverage/psi.undocumented"

    override val ignoredPackages: List<FqName> = listOf(
        FqName("org.jetbrains.kotlin.idea"),
        FqName("org.jetbrains.kotlin.lexer"),
        FqName("org.jetbrains.kotlin.parsing"),
        FqName("org.jetbrains.kotlin.kdoc.lexer"),
        FqName("org.jetbrains.kotlin.kdoc.parser"),
    )

    fun testKDocCoverage() {
        doTest()
    }
}
