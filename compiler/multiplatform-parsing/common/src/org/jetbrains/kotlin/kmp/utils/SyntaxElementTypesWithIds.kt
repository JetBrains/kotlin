/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.utils

import fleet.com.intellij.platform.syntax.SyntaxElementType
import fleet.com.intellij.platform.syntax.impl.fastutil.ints.Int2IntOpenHashMap

abstract class SyntaxElementTypesWithIds {
    companion object {
        const val NO_ID = 0
    }

    private val indexToIdMap: Int2IntOpenHashMap = Int2IntOpenHashMap()

    /**
     * Returns [NO_ID] (`0`) if an element has no associated ID.
     * It mustn't be returned for all Kotlin elements because all of they are registered using [registerElementType].
     * But it's returned for external elements or can be used for `null` element types.
     */
    fun getElementTypeId(syntaxElementType: SyntaxElementType): Int = indexToIdMap[syntaxElementType.index]

    fun registerElementType(id: Int, name: String): SyntaxElementType {
        return SyntaxElementType(name).also {
            if (indexToIdMap.containsValue(id)) {
                error("The element with id $id is already registered!")
            }
            // Don't check for the existing `index` because it's handled by syntax-api lib, and it's assumed to be initialized properly.
            indexToIdMap[it.index] = id
        }
    }
}