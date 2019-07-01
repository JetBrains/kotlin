/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.debugger.filter

import com.intellij.ui.classFilter.ClassFilter
import com.intellij.ui.classFilter.DebuggerClassFilterProvider
import org.jetbrains.kotlin.idea.debugger.KotlinDebuggerSettings

private val FILTERS = listOf(
        ClassFilter("kotlin.jvm*"),
        ClassFilter("kotlin.reflect*"),
        ClassFilter("kotlin.NoWhenBranchMatchedException"),
        ClassFilter("kotlin.TypeCastException"),
        ClassFilter("kotlin.KotlinNullPointerException")
)

class KotlinDebuggerInternalClassesFilterProvider : DebuggerClassFilterProvider {
    override fun getFilters(): List<ClassFilter>? {
        return if (KotlinDebuggerSettings.getInstance().DEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES) FILTERS else listOf()
    }
}
