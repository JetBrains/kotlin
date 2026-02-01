/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.filters

import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTest
import org.jetbrains.kotlin.analysis.test.data.manager.TestVariantChain
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestIdentifier
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Modifier

/**
 * Gets the @TestMetadata value from an annotated element.
 */
internal val AnnotatedElement.testMetadataPath: String?
    get() = getAnnotation(TestMetadata::class.java)?.value

/**
 * Gets the full test data path for a test identifier.
 * Combines class and method @TestMetadata annotations.
 */
internal val TestIdentifier.testDataPath: String?
    get() = source.orElse(null)?.testDataPath

internal val TestSource.testDataPath: String?
    get() = when (val source = this) {
        is ClassSource -> source.javaClass.testMetadataPath
        is MethodSource -> {
            val testMetadataPath = source.javaClass.testMetadataPath
            val testFileName = source.javaMethod.testMetadataPath
            if (testMetadataPath != null && testFileName != null) {
                "$testMetadataPath/$testFileName"
            } else {
                null
            }
        }

        else -> null
    }

/**
 * Gets the variant chain for a test by instantiating its test class
 * and reading the [ManagedTest.variantChain].
 */
internal val TestIdentifier.variantChain: TestVariantChain
    get() {
        val testClass = when (val source = source.orElse(null)) {
            is MethodSource -> source.javaClass
            else -> return emptyList()
        }

        val managedTestClass = testClass.findManagedTestClass() ?: return emptyList()
        val instance = managedTestClass.getDeclaredConstructor().newInstance()
        val managedTest = instance as ManagedTest
        return managedTest.variantChain
    }

/**
 * Finds the class in the hierarchy that implements [ManagedTest], if any.
 *
 * Traverses the enclosing class chain for non-static (inner) classes,
 * since static nested classes have independent lifecycle.
 *
 * @return The class implementing [ManagedTest], or null if none found.
 */
internal fun Class<*>.findManagedTestClass(): Class<*>? {
    var current: Class<*>? = this
    while (current != null) {
        if (ManagedTest::class.java.isAssignableFrom(current)) {
            return current
        }

        current = if (!Modifier.isStatic(current.modifiers)) {
            current.enclosingClass
        } else {
            null
        }
    }

    return null
}

/**
 * Formats test identifier as "ClassName.methodName" for display.
 *
 * Parses the JUnit uniqueId format to extract class and method names.
 * Falls back to [TestIdentifier.getDisplayName] if parsing fails (e.g., different JUnit engine or version).
 */
internal fun TestIdentifier.formatTestName(): String = when (val source = source?.orElse(null)) {
    is MethodSource -> "${source.javaClass.name.substringAfterLast('.')}.${source.methodName}"
    else -> displayName
}
