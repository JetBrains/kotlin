/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.js

import java.nio.file.Path

public class IncrementalModuleEntry(
    public val name: String,
    public val buildDir: Path,
    public val buildHistoryFile: Path,
)