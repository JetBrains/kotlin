/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.multiplatform.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLBlackBoxTest
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestConfiguration
import java.io.File

abstract class AbstractLLCommonBlackBoxTest : AbstractLLBlackBoxTest(
    CommonPlatforms.defaultCommonPlatform
) {
    override fun shouldSkipTest(filePath: String, configuration: TestConfiguration): Boolean {
        val testDataFile = File(filePath)
        return shouldSkipTestForCommonBoxTests(testDataFile)
    }
}


/**
 * Compiler black box tests do not support COMMON target platform.
 * As a workaround, we need to check if there is at least one incompatible pair of target platforms
 * the test is supposed to pass on.
 */
internal fun shouldSkipTestForCommonBoxTests(testDataFile: File): Boolean {
    fun TargetBackend.getEndCompatibleWith(): TargetBackend {
        var endCompatibleWith = compatibleWith
        while (endCompatibleWith.compatibleWith != TargetBackend.ANY) {
            endCompatibleWith = endCompatibleWith.compatibleWith
        }
        return endCompatibleWith
    }

    val allPassingTargets = TargetBackend.entries.filter { targetBackend ->
        targetBackend != TargetBackend.ANY &&
                InTextDirectivesUtils.isPassingTarget(targetBackend, testDataFile)
    }.map {
        it.getEndCompatibleWith()
    }.distinct()

    return allPassingTargets.size == 1
}