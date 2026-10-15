/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal.arguments

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.ProfileCompilerCommand
import org.jetbrains.kotlin.buildtools.internal.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

internal fun K2JVMCompilerArguments.applyProfileCompilerCommand(profileCompilerCommand: ProfileCompilerCommandImpl?) {
    this.profileCompilerCommand = profileCompilerCommand?.let {
        "${it.profilerPath.absolutePathStringOrThrow()}${File.pathSeparator}${it.command}${File.pathSeparator}${it.outputDir.absolutePathStringOrThrow()}"
    }
}

internal fun applyProfileCompilerCommand(
    @Suppress("unused") currentValue: ProfileCompilerCommandImpl?,
    compilerArgs: K2JVMCompilerArguments,
): ProfileCompilerCommandImpl? {
    val stringValue = compilerArgs.profileCompilerCommand ?: return null
    val parts = stringValue.split(File.pathSeparator)
    if (parts.size != 3) {
        throw CompilerArgumentsParseException("Invalid -Xprofile format: $stringValue")
    }

    return ProfileCompilerCommandImpl(Path(parts[0]), parts[1], Path(parts[2]))
}

@Serializable
internal class ProfileCompilerCommandImpl(
    val profilerPath: Path,
    val command: String,
    val outputDir: Path,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProfileCompilerCommandImpl) return false
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

    @OptIn(ExperimentalCompilerArgument::class)
    internal fun toApi(): ProfileCompilerCommand = ProfileCompilerCommand(profilerPath, command, outputDir)
}

@OptIn(ExperimentalCompilerArgument::class)
internal fun ProfileCompilerCommand.toImpl(): ProfileCompilerCommandImpl = ProfileCompilerCommandImpl(profilerPath, command, outputDir)
