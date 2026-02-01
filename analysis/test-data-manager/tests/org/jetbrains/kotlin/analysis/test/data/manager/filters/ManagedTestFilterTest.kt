/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.filters

import org.jetbrains.kotlin.analysis.test.data.manager.fakes.analysis.FakeGoldenAnalysisApiTestGenerated
import org.junit.jupiter.api.Test

internal class ManagedTestFilterTest : AbstractPostDiscoveryFilterTest() {
    @Test
    fun `ClassSource with class implementing ManagedTest is included`() {
        assertIncluded(
            filter = ManagedTestFilter,
            descriptor = descriptorFromClass<FakeGoldenAnalysisApiTestGenerated>(),
        )
    }

    @Test
    fun `ClassSource with class NOT implementing ManagedTest is excluded`() {
        assertExcluded(
            filter = ManagedTestFilter,
            descriptor = descriptorFromClass<NoMetadataClass>(),
        )
    }

    @Test
    fun `MethodSource with method from class implementing ManagedTest is included`() {
        assertIncluded(
            filter = ManagedTestFilter,
            descriptor = descriptorFromMethod(FakeGoldenAnalysisApiTestGenerated::testSymbols),
        )
    }

    @Test
    fun `MethodSource with method from class NOT implementing ManagedTest is excluded`() {
        assertExcluded(
            filter = ManagedTestFilter,
            descriptor = descriptorFromMethod(NoMetadataClass::someMethod),
        )
    }

    @Test
    fun `Nested class within ManagedTest is included`() {
        assertIncluded(
            filter = ManagedTestFilter,
            descriptor = descriptorFromClass<FakeGoldenAnalysisApiTestGenerated.Expressions>(),
        )
    }

    @Test
    fun `Nested class within non-ManagedTest is excluded`() {
        assertExcluded(
            filter = ManagedTestFilter,
            descriptor = descriptorFromClass<NoMetadataClass.Nested>(),
        )
    }

    @Test
    fun `Method in nested class within ManagedTest is included`() {
        assertIncluded(
            filter = ManagedTestFilter,
            descriptor = descriptorFromMethod(FakeGoldenAnalysisApiTestGenerated.Expressions::testCall),
        )
    }

    @Test
    fun `Method in nested class within non-ManagedTest is excluded`() {
        assertExcluded(
            filter = ManagedTestFilter,
            descriptor = descriptorFromMethod(NoMetadataClass.Nested::nestedMethod),
        )
    }

    @Test
    fun `Descriptor without source is excluded`() {
        assertExcluded(
            filter = ManagedTestFilter,
            descriptor = descriptorWithSource(source = null),
        )
    }

    @Test
    fun `Unsupported source type is excluded`() {
        val unsupportedSource = object : org.junit.platform.engine.TestSource {}
        assertExcluded(
            filter = ManagedTestFilter,
            descriptor = descriptorWithSource(source = unsupportedSource),
        )
    }

    @Test
    fun `Static nested class within ManagedTest is excluded`() {
        assertExcluded(
            filter = ManagedTestFilter,
            descriptor = descriptorFromClass<FakeGoldenAnalysisApiTestGenerated.StaticNested>(),
        )
    }

    @Test
    fun `Method in static nested class within ManagedTest is excluded`() {
        assertExcluded(
            filter = ManagedTestFilter,
            descriptor = descriptorFromMethod(FakeGoldenAnalysisApiTestGenerated.StaticNested::testStatic),
        )
    }
}
