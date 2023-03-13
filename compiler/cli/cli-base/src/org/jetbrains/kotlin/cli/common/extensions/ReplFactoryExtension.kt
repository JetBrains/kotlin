/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.extensions

import com.intellij.core.JavaCoreProjectEnvironment
import org.jetbrains.kotlin.cli.common.repl.ReplCompiler
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import java.io.File

interface ReplFactoryExtension {
    companion object : ProjectExtensionDescriptor<ReplFactoryExtension>(
        "org.jetbrains.kotlin.replFactoryExtension",
        ReplFactoryExtension::class.java
    )

    fun makeReplCompiler(
        templateClassName: String,
        templateClasspath: List<File>,
        baseClassLoader: ClassLoader?,
        configuration: CompilerConfiguration,
        projectEnvironment: JavaCoreProjectEnvironment
    ): ReplCompiler
}
