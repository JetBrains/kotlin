/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.fakes.packagecheck

import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTest
import org.jetbrains.kotlin.analysis.test.data.manager.fakes.packagebase.FakePackageCheckBaseGenerated
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** A generated managed test that `ManagedTestPackageCheckTest` runs with the packages of its module, as the `test` task does. */
class FakePackageCheckTestGenerated : ManagedTest {
    @Test
    fun testTop() {
    }

    @Nested
    inner class Inner {
        @Test
        fun testInner() {
        }
    }
}

/** A class which isn't a generated test itself, with one nested, which JUnit discovers on its own. */
class FakePackageCheckHolder {
    class StandaloneGenerated : ManagedTest {
        @Test
        fun testStandalone() {
        }
    }
}

/** A generated test whose nested test comes from a generated base class in another package, which isn't declared. */
class FakeInheritingTestGenerated : FakePackageCheckBaseGenerated() {
    @Test
    fun testOwn() {
    }
}
