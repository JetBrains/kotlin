/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.compilation

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinLogger

/**
 * Common part regardless of the platform and compilation mode of Kotlin compilation options.
 */
@ExperimentalBuildToolsApi
sealed class CompilationOptions(
    val logger: KotlinLogger?
)

/**
 * Options for incremental compilation mode.
 */
@ExperimentalBuildToolsApi
interface IncrementalCompilationOptions

/**
 * Options for non-incremental compilation mode.
 */
@ExperimentalBuildToolsApi
interface NonIncrementalCompilationOptions

/**
 * Compilation options specific for the JVM platform compiler in non-incremental mode
 */
@ExperimentalBuildToolsApi
abstract class JvmCompilationOptions(
    logger: KotlinLogger?,
    val kotlinScriptExtensions: List<String>,
) : CompilationOptions(logger)

@ExperimentalBuildToolsApi
class NonIncrementalJvmCompilationOptions(
    logger: KotlinLogger? = null,
    kotlinScriptExtensions: List<String>,
) : JvmCompilationOptions(logger, kotlinScriptExtensions), NonIncrementalCompilationOptions

/**
 * Compilation options specific for the JS platform compiler
 */
@ExperimentalBuildToolsApi
abstract class JsCompilationOptions(
    logger: KotlinLogger?,
) : CompilationOptions(logger)

/**
 * Compilation options specific for the JS platform compiler in non-incremental mode
 */
@ExperimentalBuildToolsApi
class NonIncrementalJsCompilationOptions(
    logger: KotlinLogger? = null,
) : JsCompilationOptions(logger), NonIncrementalCompilationOptions

/**
 * Compilation options specific for the metadata (aka shared across platforms code) compiler
 */
@ExperimentalBuildToolsApi
abstract class MetadataCompilationOptions(
    logger: KotlinLogger?,
) : CompilationOptions(logger)

/**
 * Compilation options specific for the metadata (aka shared across platforms code) compiler in non-incremental mode
 */
@ExperimentalBuildToolsApi
class NonIncrementalMetadataCompilationOptions(
    logger: KotlinLogger? = null,
) : MetadataCompilationOptions(logger), NonIncrementalCompilationOptions