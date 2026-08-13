/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Marker service recording that this batch's executable was linked together with the result-collecting driver generated
 * by [GroupedTestsResultProtocol.generateResultCollectingRunnerSource]. The runner of the same batch consults it to know
 * that a structured result block must appear in the VM output, so that a run printing none is reported as having
 * executed no test at all instead of passing silently.
 *
 * The grouping-stage facade has to record this rather than the runner re-deriving it: whether a batch gets the driver or
 * is linked as a standalone box-export test is decided per pipeline — on isolation for the CLI facade of the
 * KLIB-compatibility tests, on batch size for the in-process one — while the runners are shared between the two. In
 * particular the batch size does not answer it: a test that merely ended up alone in its batch (a unique batch token
 * rather than isolation) is still driven by the driver.
 */
private object GroupedTestsDriverMarker : TestService

private val TestServices.groupedTestsDriverMarker: GroupedTestsDriverMarker? by TestServices.nullableTestServiceAccessor()

/** Records that this batch's executable carries the generated result-collecting driver. */
fun TestServices.markGroupedTestsDriverGenerated() {
    register(GroupedTestsDriverMarker::class, GroupedTestsDriverMarker)
}

/** `true` when the batch was linked with the generated driver, i.e. its VM output has to carry a result block. */
val TestServices.hasGroupedTestsDriver: Boolean
    get() = groupedTestsDriverMarker != null
