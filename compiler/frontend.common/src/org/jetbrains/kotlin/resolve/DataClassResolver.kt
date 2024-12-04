/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.builtins.StandardNames.DATA_CLASS_COMPONENT_PREFIX
import org.jetbrains.kotlin.builtins.StandardNames.DATA_CLASS_COPY
import org.jetbrains.kotlin.name.Name

object DataClassResolver {
    fun createComponentName(index: Int): Name = Name.identifier(DATA_CLASS_COMPONENT_PREFIX + index)

    fun getComponentIndex(componentName: String): Int = componentName.substring(DATA_CLASS_COMPONENT_PREFIX.length).toInt()

    fun isComponentLike(name: Name): Boolean = isComponentLike(name.asString())

    fun isComponentLike(name: String): Boolean {
        if (!name.startsWith(DATA_CLASS_COMPONENT_PREFIX)) return false

        try {
            getComponentIndex(name)
        } catch (e: NumberFormatException) {
            return false
        }

        return true
    }

    fun isCopy(name: Name): Boolean = name == DATA_CLASS_COPY
}
