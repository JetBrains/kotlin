/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.formatter

import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettingsManager

/**
 * Method copyFrom is absent in 173.
 * BUNCH: 181
 */
fun CommonCodeStyleSettings.copyFromEx(source: CommonCodeStyleSettings) {
    @Suppress("IncompatibleAPI")
    CommonCodeStyleSettingsManager.copy(source, this)
}