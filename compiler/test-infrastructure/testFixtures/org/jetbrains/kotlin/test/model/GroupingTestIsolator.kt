/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

/**
 * This service is used to determine how tests would be grouped in batches in the grouped test engine.
 * For each test the engine computes tokens from all [GroupingTestIsolator] and then forms groups
 *   which have the same token sets.
 * If at least one token for the test was [BatchToken.Isolated], then the test would run in an isolated batch.
 */
abstract class GroupingTestIsolator(val testServices: TestServices, val affectsFileGenerators: Boolean) : ServicesAndDirectivesContainer {
    abstract fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken

    abstract class BatchToken {
        object Regular : BatchToken()
        object Isolated : BatchToken()
        class Custom(val name: String) : BatchToken()
    }
}
