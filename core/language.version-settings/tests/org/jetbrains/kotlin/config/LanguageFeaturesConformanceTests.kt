/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import kotlin.test.fail

class LanguageFeaturesConformanceTests {
    @Test
    fun testLanguageFeatureOrder() {
        val comparator = compareBy(nullsLast(), LanguageFeature::sinceVersion)

        if (LanguageFeature.entries.sortedWith(comparator) != LanguageFeature.entries) {
            val [a, b] = LanguageFeature.entries.zipWithNext().first { [a, b] -> comparator.compare(a, b) > 0 }
            fail(
                "Please make sure LanguageFeature entries are sorted by sinceVersion to improve readability & reduce confusion.\n" +
                        "The feature $a is out of order; its sinceVersion is ${a.sinceVersion}, yet it comes before $b, whose " +
                        "sinceVersion is ${b.sinceVersion}.\n"
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

    @Test
    fun testLanguageFeatureProperties() {
        Assertions.assertAll(
            LanguageFeature.entries.flatMap {
                listOf(
                    Executable {
                        Assertions.assertFalse(it.testOnly && it.sinceVersion != null) {
                            "$it: should be enabled by default since version ${it.sinceVersion} but is test only"
                        }
                    },
                    Executable {
                        Assertions.assertFalse(!it.forcesPreReleaseBinaries && it.forcesPreReleaseBinariesBefore != null) {
                            "$it: forcesPreReleaseBinariesBefore is not null but forcesPreReleaseBinaries is false"
                        }
                    },
                    Executable {
                        Assertions.assertFalse(it.sinceVersion != null && it.enabledInLatestLVTests) {
                            "$it: already enabled in latest language version tests, no need in '${it::enabledInLatestLVTests.name} = true'"
                        }
                    },
                )
            }
        )
    }
}
