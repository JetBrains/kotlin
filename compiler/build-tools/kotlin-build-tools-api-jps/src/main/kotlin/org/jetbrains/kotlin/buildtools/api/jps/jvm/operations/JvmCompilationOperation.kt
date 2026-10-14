/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jps.jvm.operations

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.JvmJpsManagedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerIncrementalCompilationComponents
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation

/**
 * Creates the configuration object for an incremental build whose state is owned by JPS.
 * May be used to configure incremental compilation as follows:
 * ```
 * val icConfig = operation.jpsManagedIcConfigurationBuilder(incrementalCompilationComponents)
 *
 * icConfig[JvmJpsManagedIncrementalCompilationConfiguration.LOOKUP_TRACKER] = lookupTracker
 *
 * operation[JvmCompilationOperation.INCREMENTAL_COMPILATION] = icConfig.build()
 * ```
 *
 * @param incrementalCompilationComponents what earlier compilations recorded for each module taking part in
 *   this compilation
 * @see org.jetbrains.kotlin.buildtools.api.jps.jvm.JvmJpsManagedIncrementalCompilationConfiguration
 * @since 2.5.0
 */
@InternalBuildToolsApi
@ExperimentalBuildToolsApi
public fun JvmCompilationOperation.Builder.jpsManagedIcConfigurationBuilder(
    incrementalCompilationComponents: CompilerIncrementalCompilationComponents,
): JvmJpsManagedIncrementalCompilationConfiguration.Builder {
    // The builder may be a compatibility wrapper loaded by the API classloader; unwrap it to reach the implementation
    val base: JvmCompilationOperation.Builder? = try {
        javaClass.getDeclaredField("base").also { it.isAccessible = true }.get(this) as JvmCompilationOperation.Builder
    } catch (_: NoSuchFieldException) {
        null
    }

    if (base != null) {
        return base.jpsManagedIcConfigurationBuilder(incrementalCompilationComponents)
    }

    return try {
        javaClass.classLoader.loadClass("org.jetbrains.kotlin.buildtools.internal.jps.JvmJpsManagedIncrementalCompilationConfigurationImpl")
            .getConstructor(CompilerIncrementalCompilationComponents::class.java)
            .newInstance(incrementalCompilationComponents) as JvmJpsManagedIncrementalCompilationConfiguration.Builder
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException(
            "The classpath contains no implementation for ${JvmJpsManagedIncrementalCompilationConfiguration.Builder::class.qualifiedName}",
            e,
        )
    }
}
