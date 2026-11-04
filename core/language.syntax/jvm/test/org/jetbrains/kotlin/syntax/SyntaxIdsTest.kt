/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

import org.junit.jupiter.api.Test
import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyntaxIdsTest {
    @Test
    fun testIdsAreUniqueAndContiguous() {
        val ids = buildMap {
            for (holder in listOf(
                BasicSyntaxElementTypeIds::class,
                KotlinSyntaxTokenIds::class,
                KotlinSyntaxNodeTypeIds::class,
                KDocSyntaxTokenIds::class,
                KDocSyntaxNodeTypeIds::class,
            )) {
                val properties = holder.declaredMemberProperties
                assertTrue(properties.isNotEmpty(), "No IDs found in ${holder.simpleName}")
                for (property in properties) {
                    val name = "${holder.simpleName}.${property.name}"
                    assertConst(property.isConst, name)
                    assertNull(put(name, property.getter.call() as Int), "Duplicate name $name")
                }
            }
        }

        val duplicates = ids.entries.groupBy({ it.value }, { it.key }).filterValues { it.size > 1 }
        assertTrue(duplicates.isEmpty(), "Duplicate IDs: $duplicates")

        val sortedIds = ids.values.sorted()
        assertEquals((BasicSyntaxElementTypeIds.NO_ID..sortedIds.last()).toList(), sortedIds, "IDs must form a contiguous range starting from NO_ID")
    }

    /**
     * A non-`const` ID would be computed in the static initializer of its holder. Since the first ID of each holder refers to
     * the last ID of the previous one, it could then be read as `0` from a not yet initialized holder.
     */
    private fun assertConst(isConst: Boolean, name: String) {
        assertTrue(isConst, "$name must be `const`")
    }
}
