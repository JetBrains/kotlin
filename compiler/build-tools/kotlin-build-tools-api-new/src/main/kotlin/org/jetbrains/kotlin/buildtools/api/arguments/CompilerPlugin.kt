/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.arguments

import java.nio.file.Path

public class CompilerPlugin(
    public val jarPath: Path,
    public val pluginId: String,
    public val options: Map<String, String>,
)