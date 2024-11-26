/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import java.io.Serializable

/**
 * Lists configurable feature toggles for Incremental Compilation.
 * Obsolete features might be removed, so it's not recommended
 * to use this outside of Kotlin repository.
 *
 * It's intended to be used across all build systems and target platforms.
 * Then, for example, if we're ready to port a feature from one platform to another,
 * we just have to initialize the build system-dependent property, and the value
 * would be available at the use site without any additional boilerplate.
 *
 * To add a new feature toggle, consider this checklist:
 *
 * 1. Gradle input: see [AbstractKotlinCompile.makeIncrementalCompilationFeatures].
 *    Platform-agnostic properties should be the default, but you can extend one of the subclasses, if needed.
 *    If new property might affect the result of compilation, annotate it with @Input.
 * 2. Gradle.properties support: declare the property in [org.jetbrains.kotlin.gradle.plugin.PropertiesProvider].
 *    Add its use in [org.jetbrains.kotlin.gradle.tasks.configuration.AbstractKotlinCompileConfig] or one of its subclasses (see step 1).
 * 3. Build Tools Api, KGP to Interface part: see [BuildToolsApiCompilationWork.performCompilation]
 *    You need to update both the interface [IncrementalJvmCompilationConfiguration] and its implementation(s).
 * 4. Build Tools Api, Interface to Implementation part: see [ICConfiguration.extractIncrementalCompilationFeatures] and its uses.
 *    Most likely you just need to update the `extract` method.
 * 5. Maven - TODO(emazhukin) will do and describe in KT-63837
 * 6. Gradle Integration Tests - add new option to [org.jetbrains.kotlin.gradle.testbase.BuildOptions]
 */
data class IncrementalCompilationFeatures(
    /**
     * Snapshot-based cross-module IC if true, BuildHistory-based if false.
     * Snapshot-based IC is only available in JVM.
     */
    val withAbiSnapshot: Boolean = false,
    /**
     * Enables [RecoverableCompilationTransaction] for better restoration of build outputs
     * in case of a build error.
     */
    val preciseCompilationResultsBackup: Boolean = false,
    /**
     * If enabled, IC caches are flushed to FS only when the compilation is successful.
     * Optimizes outputs backup on Gradle side.
     * Requires [preciseCompilationResultsBackup]
     */
    val keepIncrementalCompilationCachesInMemory: Boolean = false,
    /**
     * By default, with k2 KMP, we recompile the whole module, if any common sources are recompiled.
     * It provides consistent builds at the cost of compilation speed. (See KT-62686 for the underlying issue.)
     * You can enable "unsafeIC" to use pre-2.0 behavior with potentially incorrect incremental builds.
     */
    val enableUnsafeIncrementalCompilationForMultiplatform: Boolean = false,
    /**
     * Scope expansion policy governs the cases where incremental compilation uses multiple compilation steps.
     *
     * For example, suppose that we compile a.kt incrementally, and we find out that it has introduced
     * an overload of fun fooBar in package org.example.
     * Now we need to recompile usages of org.example.fooBar to ensure that they're linked against the most appropriate
     * overload visible to them. Suppose that all these usages are in b.kt.
     *
     * Then, in Monotonous mode the next compilation step would include both a.kt and b.kt.
     * And in the original mode the next compilation step would include only the files that weren't compiled previously.
     */
    val enableMonotonousIncrementalCompileSetExpansion: Boolean = false,
) : Serializable {

    companion object {
        val DEFAULT_CONFIGURATION = IncrementalCompilationFeatures()

        const val serialVersionUID: Long = 2
    }
}
