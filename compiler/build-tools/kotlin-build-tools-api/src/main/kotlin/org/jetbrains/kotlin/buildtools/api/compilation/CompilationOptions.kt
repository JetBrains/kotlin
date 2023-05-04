/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.compilation

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinLogger

@ExperimentalBuildToolsApi
sealed class CompilationOptions(
    val logger: KotlinLogger?
)

@ExperimentalBuildToolsApi
abstract class JvmCompilationOptions(
    logger: KotlinLogger?,
    val kotlinScriptExtensions: List<String>,
) : CompilationOptions(logger)

@ExperimentalBuildToolsApi
interface IncrementalCompilationOptions

@ExperimentalBuildToolsApi
interface NonIncrementalCompilationOptions

@ExperimentalBuildToolsApi
class NonIncrementalJvmCompilationOptions(
    logger: KotlinLogger? = null,
    kotlinScriptExtensions: List<String>,
) : JvmCompilationOptions(logger, kotlinScriptExtensions), NonIncrementalCompilationOptions

@ExperimentalBuildToolsApi
abstract class JsCompilationOptions(
    logger: KotlinLogger?,
) : CompilationOptions(logger)

@ExperimentalBuildToolsApi
class NonIncrementalJsCompilationOptions(
    logger: KotlinLogger? = null,
) : JsCompilationOptions(logger), NonIncrementalCompilationOptions

@ExperimentalBuildToolsApi
abstract class MetadataCompilationOptions(
    logger: KotlinLogger?,
) : CompilationOptions(logger)

@ExperimentalBuildToolsApi
class NonIncrementalMetadataCompilationOptions(
    logger: KotlinLogger? = null,
) : MetadataCompilationOptions(logger), NonIncrementalCompilationOptions