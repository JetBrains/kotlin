/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.extensions

import com.intellij.core.JavaCoreProjectEnvironment
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor

interface ShellExtension {
    companion object : ProjectExtensionDescriptor<ShellExtension>(
        "org.jetbrains.kotlin.shellExtension",
        ShellExtension::class.java
    )

    fun isAccepted(arguments: CommonCompilerArguments): Boolean

    fun run(
        arguments: CommonCompilerArguments,
        configuration: CompilerConfiguration,
        projectEnvironment: JavaCoreProjectEnvironment
    ): ExitCode
}
