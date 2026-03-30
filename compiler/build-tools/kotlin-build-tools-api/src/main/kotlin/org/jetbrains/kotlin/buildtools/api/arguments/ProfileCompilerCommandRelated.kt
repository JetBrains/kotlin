/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.arguments

import java.nio.file.Path

/**
 * @since 2.4.0
 */
@ExperimentalCompilerArgument
public class ProfileCompilerCommand(
    public val profilerPath: Path,
    public val command: String,
    public val outputDir: Path,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProfileCompilerCommand) return false
        if (profilerPath != other.profilerPath) return false
        if (command != other.command) return false
        return outputDir == other.outputDir
    }

    override fun hashCode(): Int {
        var result = profilerPath.hashCode()
        result = 31 * result + command.hashCode()
        result = 31 * result + outputDir.hashCode()
        return result
    }

    override fun toString(): String = "ProfileCompilerCommand(profilerPath=$profilerPath, command=$command, outputDir=$outputDir)"
}