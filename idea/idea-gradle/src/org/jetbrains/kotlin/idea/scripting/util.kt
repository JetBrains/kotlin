/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting

import com.intellij.openapi.util.registry.Registry

const val minimal_gradle_version_supported = "6.0"

fun shouldLoadDependenciesDuringImport(): Boolean {
    return Registry.`is`("kotlin.gradle.scripts.useIdeaProjectImport", false)
}