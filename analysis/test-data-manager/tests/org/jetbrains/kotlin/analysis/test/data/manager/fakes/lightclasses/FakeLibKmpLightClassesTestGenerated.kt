/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.fakes.lightclasses


import org.jetbrains.kotlin.analysis.test.data.manager.fakes.FakeManagedTest
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@TestMetadata("testData/lightClasses")
class FakeLibKmpLightClassesTestGenerated : FakeManagedTest() {
    override val variantChain = listOf("lib", "kmp.lib")

    @Test
    @TestMetadata("simple.kt")
    fun testSimple() {
    }

    @Nested
    @TestMetadata("testData/lightClasses/nestedDir")
    inner class NestedDir {
        @Test
        @TestMetadata("inner.kt")
        fun testInner() {
        }
    }
}
