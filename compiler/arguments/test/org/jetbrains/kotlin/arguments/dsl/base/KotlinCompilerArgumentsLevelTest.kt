/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.base

import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KotlinCompilerArgumentsLevelTest {

    @Test
    fun shouldRejectMergingLevelsWithDifferentNames() {
        val level1 by compilerArgumentsLevel("one") {}
        val level2 by compilerArgumentsLevel("two") {}

        assertFailsWith<IllegalArgumentException>(
            message = "Names for compiler arguments level should be the same! We are trying to merge one with two"
        ) {
            level1.mergeWith(level2)
        }
    }

    @Test
    fun shouldMergeCompilerArguments() {
        val level1 by compilerArgumentsLevel("one") {
            addCompilerArguments(stubCompilerArgument("one_one"))
        }
        val level2 by compilerArgumentsLevel("one") {
            addCompilerArguments(stubCompilerArgument("one_two"))
        }

        val merged = level1.mergeWith(level2)

        assertEquals(2, merged.arguments.size)
        assertEquals(0, merged.nestedLevels.size)
    }

    @Test
    fun shouldPreventMergingCompilerArgumentsWithSameNames() {
        val level1 by compilerArgumentsLevel("one") {
            addCompilerArguments(stubCompilerArgument("one_one"))
        }
        val level2 by compilerArgumentsLevel("one") {
            addCompilerArguments(stubCompilerArgument("one_one"))
        }

        assertFailsWith<IllegalArgumentException>(
            message = "Both levels with name one contain compiler arguments with the same name(s): one_one"
        ) {
            level1.mergeWith(level2)
        }
    }

    @Test
    fun shouldMergeNestedLevelsWithTheSameName() {
        val level1 by compilerArgumentsLevel("one") {
            subLevel("one_one") {
                addCompilerArguments(stubCompilerArgument("one_one_one"))
            }
        }
        val level2 by compilerArgumentsLevel("one") {
            subLevel("one_one") {
                addCompilerArguments(stubCompilerArgument("one_one_two"))
            }
        }

        val merged = level1.mergeWith(level2)

        assertEquals(0, merged.arguments.size)
        assertEquals(1, merged.nestedLevels.size)
        assertEquals(2, merged.nestedLevels.first().arguments.size)
    }

    @Test
    fun shouldNotMergeNestedLevelsWithDifferentNames() {
        val level1 by compilerArgumentsLevel("one") {
            subLevel("one_one") {
                addCompilerArguments(stubCompilerArgument("one_one_one"))
            }
        }
        val level2 by compilerArgumentsLevel("one") {
            subLevel("one_two") {
                addCompilerArguments(stubCompilerArgument("one_two_one"))
            }
        }

        val merged = level1.mergeWith(level2)

        assertEquals(0, merged.arguments.size)
        assertEquals(2, merged.nestedLevels.size)
        assertEquals(1, merged.nestedLevels.first().arguments.size)
        assertEquals(1, merged.nestedLevels.last().arguments.size)
    }

    private fun stubCompilerArgument(
        name: String
    ) = KotlinCompilerArgument(
        name,
        description = "${name}_one".asReleaseDependent(),
        valueType = StubKotlinArgumentValueType(),
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0
        )
    )

    private class StubKotlinArgumentValueType : KotlinArgumentValueType<Unit> {
        override val isNullable: ReleaseDependent<Boolean> = true.asReleaseDependent()
        override val defaultValue: ReleaseDependent<Unit?> get() = null.asReleaseDependent()
    }

}