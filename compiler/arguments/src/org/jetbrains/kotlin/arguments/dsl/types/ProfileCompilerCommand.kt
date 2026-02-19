/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
class ProfileCompilerCommand(
    val profilerPath: Path,
    val command: String,
    val outputDir: Path,
)

