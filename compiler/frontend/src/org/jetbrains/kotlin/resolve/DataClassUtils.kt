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

package org.jetbrains.kotlin.resolve.dataClassUtils

import org.jetbrains.kotlin.name.Name

private val COMPONENT_FUNCTION_NAME_PREFIX = "component"

public fun isComponentLike(name: Name): Boolean {
    if (!name.asString().startsWith(COMPONENT_FUNCTION_NAME_PREFIX)) return false

    try {
        getComponentIndex(name)
    }
    catch (e: NumberFormatException) {
        return false
    }

    return true
}

public fun getComponentIndex(componentName: Name): Int =
    componentName.asString().substring(COMPONENT_FUNCTION_NAME_PREFIX.length).toInt()

public fun createComponentName(index: Int): Name =
    Name.identifier(COMPONENT_FUNCTION_NAME_PREFIX + index)
