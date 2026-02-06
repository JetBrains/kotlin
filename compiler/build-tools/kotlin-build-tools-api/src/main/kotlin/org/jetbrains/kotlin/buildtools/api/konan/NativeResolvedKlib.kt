/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.konan

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import java.nio.file.Path

@ExperimentalBuildToolsApi
public interface NativeResolvedKlib {
    public val location: Path
    public val dependencies: Set<NativeResolvedKlib>
}