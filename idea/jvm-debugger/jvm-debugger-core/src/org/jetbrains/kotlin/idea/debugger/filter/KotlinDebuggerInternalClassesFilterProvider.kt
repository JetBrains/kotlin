/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.filter

import com.intellij.ui.classFilter.ClassFilter
import com.intellij.ui.classFilter.DebuggerClassFilterProvider
import org.jetbrains.kotlin.idea.debugger.KotlinDebuggerSettings

class KotlinDebuggerInternalClassesFilterProvider : DebuggerClassFilterProvider {
    private companion object {
        private val FILTERS = listOf(
            ClassFilter("kotlin.jvm*"),
            ClassFilter("kotlin.reflect*"),
            ClassFilter("kotlin.NoWhenBranchMatchedException"),
            ClassFilter("kotlin.TypeCastException"),
            ClassFilter("kotlin.KotlinNullPointerException")
        )
    }

    override fun getFilters(): List<ClassFilter>? {
        return if (KotlinDebuggerSettings.getInstance().disableKotlinInternalClasses) FILTERS else listOf()
    }
}