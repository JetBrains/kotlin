/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.extensions.ExtensionsArea
import java.io.File

// BUNCH: 193
fun registerExtensionPointAndExtensionsEx(pluginFile: File, fileName: String, area: ExtensionsArea) {
    @Suppress("MissingRecentApi")
    CoreApplicationEnvironment.registerExtensionPointAndExtensions(pluginFile, fileName, area)
}