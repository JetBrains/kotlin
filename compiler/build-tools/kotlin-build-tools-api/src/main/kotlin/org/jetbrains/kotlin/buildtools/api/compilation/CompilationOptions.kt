/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.compilation

import org.jetbrains.kotlin.buildtools.api.KotlinLogger

sealed class CompilationOptions(
    val logger: KotlinLogger?
)

abstract class JvmCompilationOptions(
    logger: KotlinLogger?,
    val kotlinScriptExtensions: List<String>,
) : CompilationOptions(logger)

interface IncrementalCompilationOptions

interface NonIncrementalCompilationOptions

class NonIncrementalJvmCompilationOptions(
    logger: KotlinLogger? = null,
    kotlinScriptExtensions: List<String>,
) : JvmCompilationOptions(logger, kotlinScriptExtensions), NonIncrementalCompilationOptions

abstract class JsCompilationOptions(
    logger: KotlinLogger?,
) : CompilationOptions(logger)

class NonIncrementalJsCompilationOptions(
    logger: KotlinLogger? = null,
) : JsCompilationOptions(logger), NonIncrementalCompilationOptions

abstract class MetadataCompilationOptions(
    logger: KotlinLogger?,
) : CompilationOptions(logger)

class NonIncrementalMetadataCompilationOptions(
    logger: KotlinLogger? = null,
) : MetadataCompilationOptions(logger), NonIncrementalCompilationOptions