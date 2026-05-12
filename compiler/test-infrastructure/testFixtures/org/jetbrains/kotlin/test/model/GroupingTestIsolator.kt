/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import java.util.WeakHashMap

/**
 * This service is used to determine how tests would be grouped in batches in the grouped test engine.
 * For each test the engine computes tokens from all [GroupingTestIsolator] and then forms groups
 *   which have the same token sets.
 * If at least one token for the test was [BatchToken.Isolated], then the test would run in an isolated batch.
 */
abstract class GroupingTestIsolator(val testServices: TestServices, val affectsFileGenerators: Boolean) : ServicesAndDirectivesContainer {
    abstract fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken

    abstract class BatchToken {
        object Regular : BatchToken()
        object Isolated : BatchToken()
        data class Custom(val name: String) : BatchToken()
    }

    companion object {
        private val packageKotlinInternalRegex = Regex("package\\s${StandardNames.KOTLIN_INTERNAL_FQ_NAME}")
        private val classToStringRegex = Regex("::class.toString\\(\\)")
        // Detects any `.qualifiedName` property access (e.g. on a `KClass` obtained via `T::class`
        // through a reified inline helper). Tests asserting against `qualifiedName` rely on the
        // original package and would break if `BatchingPackageInserter` prepended a batch package.
        private val qualifiedNameAccessRegex = Regex("\\.qualifiedName\\b")
        private val wasmFailsInRegex = Regex("// WASM_FAILS_IN: ") // TODO KT-86384: replace with check of new test directive, into `isolationDirectives` below
        private val importKotlinReflect = Regex("import\\s+kotlin\\.reflect\\.")
        val ISOLATION_SOURCE_REGEXES = listOf(
            packageKotlinInternalRegex,
            classToStringRegex,
            qualifiedNameAccessRegex,
            wasmFailsInRegex,
            importKotlinReflect,
        )

        private val sourceContainsCache = WeakHashMap<TestModuleStructure, MutableMap<Regex, Boolean>>()
        fun TestModuleStructure.sourceContains(regex: Regex): Boolean {
            synchronized(sourceContainsCache) {
                val perStructureCache = sourceContainsCache.getOrPut(this) { mutableMapOf() }
                return perStructureCache.getOrPut(regex) {
                    modules.any { module ->
                        module.files.any { it.originalContent.contains(regex) }
                    }
                }
            }
        }
    }
}
