/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

fun isEap(version: String): Boolean {
    return version.contains("rc") || version.contains("eap") || version.contains("-M")
}

fun isDev(version: String): Boolean {
    return version.contains("dev")
}

val buildNumber: String by lazy {
    val file = File(PathUtil.kotlinPathsForIdeaPlugin.homePath, "build.txt")
    if (file.exists()) file.readText().trim() else "unknown"
}