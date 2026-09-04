/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.filters

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.platform.engine.*
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.PostDiscoveryFilter
import java.util.*
import kotlin.reflect.KFunction1

/**
 * Base class for testing [PostDiscoveryFilter] implementations.
 * Provides common assertion methods and test descriptor creation utilities.
 */
internal abstract class AbstractPostDiscoveryFilterTest {
    /**
     * A simple class without any test-related annotations or interfaces.
     * Used to test filters' behavior with non-test classes.
     */
    class NoMetadataClass {
        fun someMethod() {}

        class Nested {
            fun nestedMethod() {}
        }
    }

    protected fun assertIncluded(filter: PostDiscoveryFilter, descriptor: TestDescriptor) {
        assertIncluded(filter.apply(descriptor))
    }

    protected fun assertExcluded(filter: PostDiscoveryFilter, descriptor: TestDescriptor) {
        assertExcluded(filter.apply(descriptor))
    }

    protected fun assertIncluded(result: FilterResult) {
        assertTrue(result.included()) { "Expected included: ${result.reason}" }
    }

    protected fun assertExcluded(result: FilterResult) {
        assertTrue(result.excluded()) { "Expected excluded: ${result.reason}" }
    }

    protected inline fun <reified T> descriptorFromClass(
        type: TestDescriptor.Type = TestDescriptor.Type.TEST
    ): TestDescriptor = descriptorWithSource(ClassSource.from(T::class.java), type)

    protected inline fun <reified T> descriptorFromMethod(method: KFunction1<T, Unit>): TestDescriptor {
        val klass = T::class.java
        val javaMethod = klass.getMethod(method.name)
        return descriptorWithSource(MethodSource.from(klass, javaMethod))
    }

    protected fun descriptorWithSource(
        source: TestSource?,
        type: TestDescriptor.Type = TestDescriptor.Type.TEST
    ): TestDescriptor = object : TestDescriptor {
        override fun getUniqueId(): UniqueId = UniqueId.root("test", "stub")
        override fun getDisplayName(): String = "Stub Test"
        override fun getType(): TestDescriptor.Type = type
        override fun getSource(): Optional<TestSource> = Optional.ofNullable(source)
        override fun getParent(): Optional<TestDescriptor> = Optional.empty()
        override fun setParent(parent: TestDescriptor?) {}
        override fun getChildren(): MutableSet<out TestDescriptor> = mutableSetOf()
        override fun addChild(descriptor: TestDescriptor) {}
        override fun removeChild(descriptor: TestDescriptor) {}
        override fun removeFromHierarchy() {}
        override fun getTags(): MutableSet<TestTag> = mutableSetOf()
        override fun findByUniqueId(uniqueId: UniqueId): Optional<out TestDescriptor> = Optional.empty()
    }
}
