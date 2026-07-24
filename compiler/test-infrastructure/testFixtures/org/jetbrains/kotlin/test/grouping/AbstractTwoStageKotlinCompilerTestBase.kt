/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import org.jetbrains.kotlin.test.GroupingTestRunner
import org.jetbrains.kotlin.test.NonGroupingTestRunner

/**
 * This is a base class for all two-stage compiler tests which are executed by the [CompilerTestGroupingTestEngine].
 */
abstract class AbstractTwoStageKotlinCompilerTestBase {
    abstract val nonGroupingRunner: NonGroupingTestRunner
    abstract val nonGroupingStageRunnerInitialized: Boolean

    abstract val groupingStageRunner: GroupingTestRunner
    abstract val secondStageRunnerInitialized: Boolean
}
