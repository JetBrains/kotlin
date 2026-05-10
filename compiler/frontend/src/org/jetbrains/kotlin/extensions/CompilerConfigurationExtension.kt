/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.extensions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerConfiguration

interface CompilerConfigurationExtension {

    companion object : ProjectExtensionDescriptor<CompilerConfigurationExtension>(
        "org.jetbrains.kotlin.compilerConfigurationExtension",
        CompilerConfigurationExtension::class.java
    )

    fun updateConfiguration(project: Project, configuration: CompilerConfiguration)

    fun updateFileRegistry(project: Project) {}
}
