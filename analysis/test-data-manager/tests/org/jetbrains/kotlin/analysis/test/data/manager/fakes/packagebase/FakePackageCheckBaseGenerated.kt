/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.fakes.packagebase

import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** A base of `FakeInheritingTestGenerated` in another package, with a nested test that runs in the class extending it. */
abstract class FakePackageCheckBaseGenerated : ManagedTest {
    @Nested
    inner class Inherited {
        @Test
        fun testInherited() {
        }
    }
}
