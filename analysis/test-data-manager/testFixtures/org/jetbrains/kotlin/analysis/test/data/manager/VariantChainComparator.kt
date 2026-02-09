/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

/**
 * Comparator for ordering variant chains.
 *
 * Ordering rules:
 * 1. Empty list (golden) comes first
 * 2. Shorter chains have higher priority (fewer variants = higher priority)
 * 3. Alphabetical ordering for chains of the same length (for stability)
 *
 * Note: Within the same variant depth, the order is stable, but tests will run together
 * in the same group regardless of exact variant chain content.
 */
internal object VariantChainComparator : Comparator<List<String>> {
    override fun compare(a: List<String>, b: List<String>): Int {
        // Empty list (golden) always comes first
        if (a.isEmpty() && b.isEmpty()) return 0
        if (a.isEmpty()) return -1
        if (b.isEmpty()) return 1

        // Compare by depth first (fewer variants = higher priority)
        val sizeComparison = a.size.compareTo(b.size)
        if (sizeComparison != 0) return sizeComparison

        // Alphabetical ordering for stability within same depth
        for (i in a.indices) {
            val elementComparison = a[i].compareTo(b[i])
            if (elementComparison != 0) return elementComparison
        }

        return 0
    }
}
