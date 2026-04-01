/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.ProfileCompilerCommand
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.io.File
import kotlin.io.path.Path

@OptIn(ExperimentalCompilerArgument::class)
internal fun K2JVMCompilerArguments.applyProfileCompilerCommand(profileCompilerCommand: ProfileCompilerCommand?) {
    this.profileCompilerCommand = profileCompilerCommand?.let {
        "${it.profilerPath.absolutePathStringOrThrow()}${File.pathSeparator}${it.command}${File.pathSeparator}${it.outputDir.absolutePathStringOrThrow()}"
    }
}

@OptIn(ExperimentalCompilerArgument::class)
internal fun applyProfileCompilerCommand(
    currentValue: ProfileCompilerCommand?,
    compilerArgs: K2JVMCompilerArguments,
): ProfileCompilerCommand? {
    val stringValue = compilerArgs.profileCompilerCommand ?: return null
    val parts = stringValue.split(File.pathSeparator)
    require(parts.size == 3) { "Invalid -Xprofile format: $stringValue" }

    return ProfileCompilerCommand(Path(parts[0]), parts[1], Path(parts[2]))
}