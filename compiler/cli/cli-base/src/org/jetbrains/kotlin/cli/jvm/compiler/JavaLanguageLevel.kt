/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.pom.java.LanguageLevel

fun Project.setupHighestLanguageLevel() {
    // `JDK_X` is the highest language level the platform knows: every Java feature is enabled, including experimental ones like
    // Valhalla value classes (`-Xvalhalla-support`) or the newest released syntax like module import declarations (JEP 511,
    // needs at least `JDK_25`). The Kotlin compiler only reads Java sources, so it is safe to recognize the latest syntax
    // instead of failing on it.
    LanguageLevelProjectExtension.getInstance(this).languageLevel = LanguageLevel.JDK_X
}
