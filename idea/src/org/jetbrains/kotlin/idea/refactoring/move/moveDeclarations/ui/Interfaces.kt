/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui

import com.intellij.openapi.options.ConfigurationException

internal interface Model<out T> {
    @Throws(ConfigurationException::class)
    fun computeModelResult(throwOnConflicts: Boolean = false): T

    @Throws(ConfigurationException::class)
    fun computeModelResult(): T
}