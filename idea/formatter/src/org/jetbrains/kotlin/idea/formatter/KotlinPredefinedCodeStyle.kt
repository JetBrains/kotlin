/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.lang.Language
import com.intellij.psi.codeStyle.PredefinedCodeStyle

abstract class KotlinPredefinedCodeStyle(name: String, language: Language) : PredefinedCodeStyle(name, language) {
    abstract val codeStyleId: String
}