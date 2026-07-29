/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.pom.java.LanguageLevel

fun Project.setupHighestLanguageLevel(isValhallaSupportEnabled: Boolean = false) {
    LanguageLevelProjectExtension.getInstance(this).languageLevel = when {
        // A Project Valhalla compilation (`-Xvalhalla-support`) may reference Java value classes. `JDK_X` is the level that
        // enables `JavaFeature.VALHALLA_VALUE_CLASSES`, so a Java `value class`/`value record` in source is parsed correctly
        // (its `value` modifier is recognized).
        isValhallaSupportEnabled -> LanguageLevel.JDK_X
        else -> LanguageLevel.entries.firstOrNull { it.name == "JDK_17" }
            ?: LanguageLevel.entries.firstOrNull { it.name == "JDK_15_PREVIEW" }
            ?: LanguageLevel.JDK_X
    }
}
