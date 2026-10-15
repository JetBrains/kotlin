/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.codebaseTest

import org.jetbrains.kotlin.AbstractPackageNameTest
import org.junit.jupiter.api.Test

class AnalysisApiPlatformInterfacePackageNameTest : AbstractPackageNameTest() {
    @Test
    fun testPackageNameConsistency() = doTest()

    /**
     * DO NOT ADD NEW ENTRIES TO THE LIST
     * See KT-89733
     */
    override val ignoredFiles: Set<String> = setOf(
        "org/jetbrains/kotlin/analysis/api/platform/resolution/KaResolvableReferenceBridge.kt"
    )
}
