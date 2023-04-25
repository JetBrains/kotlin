/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.pom.java.LanguageLevel

fun Project.setupHighestLanguageLevel() {
    LanguageLevelProjectExtension.getInstance(this).languageLevel =
        LanguageLevel.values().firstOrNull { it.name == "JDK_17" }
            ?: LanguageLevel.values().firstOrNull { it.name == "JDK_15_PREVIEW" }
                    ?: LanguageLevel.JDK_X
}
