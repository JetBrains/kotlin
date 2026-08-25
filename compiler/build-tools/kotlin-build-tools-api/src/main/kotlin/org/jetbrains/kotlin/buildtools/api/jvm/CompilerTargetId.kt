/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * Identifies one of the modules taking part in a compilation.
 *
 * @property name the module name, as it is known to the build system
 * @property type distinguishes modules that share a name, such as a module's production and test parts
 * @since 2.5.20
 */
@ExperimentalBuildToolsApi
public data class CompilerTargetId(
    public val name: String,
    public val type: String,
)
