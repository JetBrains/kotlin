/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.arguments

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

internal fun <T> K2JVMCompilerArguments.applyProfileCompilerCommand(profileCompilerCommand: T?) {
    require(profileCompilerCommand is String?) { "X_PROFILE must be a string, but was $profileCompilerCommand" }

    this.profileCompilerCommand = profileCompilerCommand
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applyProfileCompilerCommand(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is String?) { "X_PROFILE home must be a string, but was $currentValue" }

    return compilerArgs.profileCompilerCommand as T
}

internal fun <T> K2JVMCompilerArguments.applyJdkHome(jdkHome: T?) {
    require(jdkHome is String?) { "JDK home must be a string, but was $jdkHome" }

    this.jdkHome = jdkHome
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applyJdkHome(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is String?) { "JDK home must be a string, but was $currentValue" }

    return compilerArgs.jdkHome as T
}