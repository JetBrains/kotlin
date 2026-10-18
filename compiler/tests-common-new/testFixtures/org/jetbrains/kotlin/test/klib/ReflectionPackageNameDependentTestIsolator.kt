/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.test.model.GroupingTestIsolator
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

class ReflectionPackageNameDependentTestIsolator(
    testServices: TestServices,
) : GroupingTestIsolator(testServices, affectsFileGenerators = true) {
    override fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken =
        if (REFLECTIVE_PACKAGE_NAME_ACCESSES.any { moduleStructure.sourceContains(it) }) BatchToken.Isolated
        else BatchToken.Regular

    companion object {
        /**
         * Besides `KClass.qualifiedName`, Kotlin/Native and Kotlin/Wasm render the qualified names in `KClass.toString()` and
         * `KType.toString()`, so a renamed test doing it cannot match the expected names either.
         * This is a heuristic: a test missed by it (e.g. comparing `Throwable.toString()` on Kotlin/Native) fails in its batch,
         * and is then retried in isolation.
         */
        private val REFLECTIVE_PACKAGE_NAME_ACCESSES = listOf(
            Regex("""\.qualifiedName\b"""),
            Regex("""::class\.toString\(\)"""),
            Regex(""""class """), // an expected result of `KClass.toString()`
            Regex("""\$\{[^}]*::class\b"""), // `KClass.toString()` in a string template
            Regex("""\btypeOf\b"""),
            Regex("""\bKType\b"""),
            Regex("""\.returnType\b"""),
            Regex("""\.type\b"""),
        )
    }
}
