/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.codebaseTest

import org.jetbrains.kotlin.AbstractPackageNameTest
import org.junit.jupiter.api.Test

class PsiApiPackageNameTest : AbstractPackageNameTest() {
    @Test
    fun testPackageNameConsistency() = doTest()

    /**
     * DO NOT ADD NEW ENTRIES TO THE LIST
     * See KT-89733
     */
    override val ignoredFiles: Set<String> = setOf(
        "org/jetbrains/kotlin/psi/addRemoveModifier.kt",
        "org/jetbrains/kotlin/psi/FindDocComment.kt",
        "org/jetbrains/kotlin/psi/TypeRefHelpers.kt"
    )
}
