/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.psi.tree.IElementType

object ElementTypeChecker {
    private const val INDEX_SUFFIX = "_INDEX"

    @JvmStatic
    fun checkExplicitStaticIndexesMatchImplicit(klass: Class<*>) {
        val explicitIndexes = mutableMapOf<String, Int>()
        val elementTypes = mutableMapOf<String, IElementType>()

        for (field in klass.fields) {
            val fieldName = field.name
            val value = field.get(klass)
            if (fieldName.endsWith(INDEX_SUFFIX)) {
                require(value is Int) { "All properties that ends with `$INDEX_SUFFIX` should be static integers. The `${fieldName}` is not." }
                explicitIndexes[fieldName] = value
            } else if (value is IElementType) {
                elementTypes[fieldName] = value
            }
        }

        for ((elementTypeName, elementType) in elementTypes) {
            val explicitIndex = explicitIndexes.remove(elementTypeName + INDEX_SUFFIX) ?: continue
            require(
                explicitIndex == elementType.index.toInt()
            ) { "Index `${elementType.index}` of `${elementTypeName}` doesn't match explicit statically declared `$explicitIndex`." }
        }

        for ((name, id) in explicitIndexes) {
            error("The declared `$name` (`$id`) doesn't have corresponding element type.")
        }
    }
}