/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.syntax.SyntaxId
import org.jetbrains.kotlin.syntax.syntaxId
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Checks that the PSI-based API in `ModifierCheckerHelpers.kt` is consistent with the parse-tree-agnostic [KotlinModifierHelpers].
 */
@OptIn(KtImplementationDetail::class)
class ModifierCheckerHelpersConsistencyTest {
    @Test
    fun featureDependencies() {
        assertMapsConsistent(KotlinModifierHelpers.featureDependencies, featureDependencies)
    }

    @Test
    fun featureDependenciesTargets() {
        assertSame(KotlinModifierHelpers.featureDependenciesTargets, featureDependenciesTargets)
    }

    @Test
    fun possibleTargetPredicateMap() {
        assertMapsConsistent(KotlinModifierHelpers.possibleTargetPredicateMap, possibleTargetPredicateMap)
    }

    @Test
    fun deprecatedTargetPredicateMap() {
        assertMapsConsistent(KotlinModifierHelpers.deprecatedTargetPredicateMap, deprecatedTargetPredicateMap)
    }

    @Test
    fun possibleParentTargetPredicateMap() {
        assertMapsConsistent(KotlinModifierHelpers.possibleParentTargetPredicateMap, possibleParentTargetPredicateMap)
    }

    @Test
    fun deprecatedParentTargetMap() {
        assertMapsConsistent(KotlinModifierHelpers.deprecatedParentTargetMap, deprecatedParentTargetMap)
    }

    @Test
    fun redundantTargetMap() {
        assertMapsConsistent(KotlinModifierHelpers.redundantTargetMap, redundantTargetMap)
    }

    @Test
    fun defaultVisibilityTargetPredicate() {
        assertSame(KotlinModifierHelpers.defaultVisibilityTargetPredicate, defaultVisibilityTargetPredicate)
    }

    @Test
    fun allowedInContextParameters() {
        assertEquals(
            KotlinModifierHelpers.allowedInContextParameters.toSet(),
            allowedInContextParameters.mapTo(mutableSetOf()) { it.syntaxId() },
        )
    }

    @Test
    fun compatibility() {
        val tokens = KtTokens.MODIFIER_KEYWORDS_ARRAY.toList() + KtTokens.OBJECT_KEYWORD
        for (first in tokens) {
            for (second in tokens) {
                assertEquals(
                    KotlinModifierHelpers.compatibility(first.syntaxId(), second.syntaxId()),
                    compatibility(first, second),
                    "Compatibility of '$first' and '$second'",
                )
            }
        }
    }

    private fun <V> assertMapsConsistent(expected: Map<SyntaxId, V>, actual: Map<out KtKeywordToken, V>) {
        assertEquals(expected.keys.toList(), actual.keys.map { it.syntaxId() }, "Keys (in order)")
        for ((key, value) in actual) {
            assertSame(expected[key.syntaxId()], value, "Value for '$key'")
        }
    }
}
