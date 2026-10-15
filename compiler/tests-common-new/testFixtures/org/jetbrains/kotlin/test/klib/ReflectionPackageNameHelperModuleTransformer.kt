/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.testInfraError

/**
 * Extracts the helper file added by [ReflectionPackageNameAdditionalSourceProvider] from the test
 * module it was attached to and places it into a dedicated module named [HELPERS_MODULE_NAME].
 *
 * Every original test module then gets a regular `DependencyKind.Binary` dependency on the new
 * helpers module, as `BatchingPackageInserter` annotates the files of all of them.
 *
 * This allows `BatchingPackageInserter` to annotate grouped backward compatibility tests
 * with the annotation `kotlin.internal.ReflectionPackageName` without it present in stdlib older than v2.5.
 * This annotation is used in backends from v2.5 to keep its original package name for the reflection.
 * The current stdlib contains this annotation as well, so it is necessary not to link the helper module into the executable,
 * to prevent `IllegalStateException: IrClassSymbolImpl is already bound. Signature: helpers/...` at link time.
 */
object ReflectionPackageNameHelperModuleTransformer : HelperModuleExtractingTransformer() {
    const val HELPERS_MODULE_NAME: String = "reflectionPackageNameHelper"

    override val helpersModuleName: String
        get() = HELPERS_MODULE_NAME

    /** [ReflectionPackageNameAdditionalSourceProvider] attaches the helper file to a single module. */
    override fun selectHelpersModuleFiles(helperFiles: List<TestFile>): List<TestFile> =
        listOf(helperFiles.singleOrNull() ?: testInfraError("Expected a single ReflectionPackageName helper file, got ${helperFiles.size}"))

    /**
     * Returns true if the given module is the dedicated helper module this transformer synthesizes.
     * A grouping-stage facade must neither collect its KLIB as a per-test output nor link it.
     */
    fun isHelperModule(module: TestModule): Boolean = module.name == HELPERS_MODULE_NAME

    /**
     * Returns true if the given file is the synthetic ReflectionPackageName helper file produced
     * by [ReflectionPackageNameAdditionalSourceProvider].
     */
    override fun isHelperFile(file: TestFile): Boolean {
        if (!file.isAdditional) return false
        // Cheap content check: helper files start with `package kotlin.internal`.
        val content = file.originalContent
        // Either it begins with `package kotlin.internal` (first line), or contains it as a
        // non-commented line at the top.
        val lineSequence = content.lineSequence()
        val firstNonEmptyLine = lineSequence.firstOrNull { it.isNotBlank() } ?: return false
        return firstNonEmptyLine.trim() == "package kotlin.internal" &&
                lineSequence.any { it.contains("annotation class ReflectionPackageName") }
    }
}
