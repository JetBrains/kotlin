/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.junit.jupiter.api.Test
import kotlin.test.fail

class LanguageFeaturesConformanceTests {
    @Test
    fun testLanguageFeatureOrder() {
        val values = enumValues<LanguageFeature>()
        val enabledFeatures = values.filter { it.sinceVersion != null }

        if (enabledFeatures.sortedBy { it.sinceVersion!! } != enabledFeatures) {
            val [a, b] = enabledFeatures.zipWithNext().first { [a, b] -> a.sinceVersion!! > b.sinceVersion!! }
            fail(
                "Please make sure LanguageFeature entries are sorted by sinceVersion to improve readability & reduce confusion.\n" +
                        "The feature $b is out of order; its sinceVersion is ${b.sinceVersion}, yet it comes after $a, whose " +
                        "sinceVersion is ${a.sinceVersion}.\n"
            )
        }
    }

    @Test
    fun testLanguageFeatureCrossChecks() {
        val collector = CrossFeatureChecksResultsCollector()
        context(collector) {
            LanguageFeature.entries.forEach { feature ->
                feature.crossFeatureChecks()
            }
        }
        if (collector.failedChecks.isNotEmpty()) {
            fail(collector.failedChecks.joinToString("\n") { it.message })
        }
    }
}
