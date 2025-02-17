/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.model.AnalysisHandler
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.runners.TestTierLabel
import org.jetbrains.kotlin.test.runners.TierPassesMarker
import org.jetbrains.kotlin.test.runners.applicableTestTiers
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

fun <A : ResultingArtifact<A>> testTierExceptionInverter(
    tierLabel: TestTierLabel,
    delegate: AnalysisHandler<A>,
): HandlerExceptionInverter<A> =
    HandlerExceptionInverter(delegate) { module ->
        val applicableTiers = testServices.moduleStructure.applicableTestTiers
        val current = applicableTiers.find { it.label == tierLabel }
            ?: error("The test infrastructure attempts to run a $tierLabel handler for a test that only supports: ${applicableTiers.joinToString()}")

        throw TierPassesMarker(current, module, delegate)
    }

fun <A : ResultingArtifact<A>> testTierExceptionInverter(
    tierLabel: TestTierLabel,
    delegate: (TestServices) -> AnalysisHandler<A>,
): (TestServices) -> HandlerExceptionInverter<A> = { services -> testTierExceptionInverter(tierLabel, delegate(services)) }
