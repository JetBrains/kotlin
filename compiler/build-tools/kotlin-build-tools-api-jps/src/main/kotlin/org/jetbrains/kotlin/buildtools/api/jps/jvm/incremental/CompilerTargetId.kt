/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental

import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi

/**
 * Identifies one of the modules taking part in a compilation.
 *
 * @property name the module name, as it is known to the build system
 * @property type distinguishes modules that share a name, such as a module's production and test parts
 *
 * @since 2.5.0
 */
@InternalBuildToolsApi
public class CompilerTargetId(
    public val name: String,
    public val type: String,
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is CompilerTargetId && name == other.name && type == other.type)

    override fun hashCode(): Int = 31 * name.hashCode() + type.hashCode()

    override fun toString(): String = "CompilerTargetId(name=$name, type=$type)"
}
