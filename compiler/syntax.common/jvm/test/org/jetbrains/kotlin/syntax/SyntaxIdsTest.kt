/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

import org.jetbrains.kotlin.BasicElementTypeId
import org.jetbrains.kotlin.KDocNodeTypeId
import org.jetbrains.kotlin.KDocTokenId
import org.jetbrains.kotlin.KtNodeTypeId
import org.jetbrains.kotlin.KtTokenId
import org.jetbrains.kotlin.NO_ID
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyntaxIdsTest {
    @Test
    fun testIdsAreUniqueAndContiguous() {
        val ids = buildMap {
            put("NO_ID", NO_ID)
            for (holder in listOf(BasicElementTypeId::class, KtTokenId::class, KtNodeTypeId::class, KDocTokenId::class, KDocNodeTypeId::class)) {
                val fields = holder.java.declaredFields.filter { Modifier.isStatic(it.modifiers) && it.type == Int::class.javaPrimitiveType }
                assertTrue(fields.isNotEmpty(), "No IDs found in ${holder.simpleName}")
                for (field in fields) {
                    put("${holder.simpleName}.${field.name}", field.getInt(null))
                }
            }
        }

        val duplicates = ids.entries.groupBy({ it.value }, { it.key }).filterValues { it.size > 1 }
        assertTrue(duplicates.isEmpty(), "Duplicate IDs: $duplicates")

        val sortedIds = ids.values.sorted()
        assertEquals((NO_ID..sortedIds.last()).toList(), sortedIds, "IDs must form a contiguous range starting from NO_ID")
    }
}
