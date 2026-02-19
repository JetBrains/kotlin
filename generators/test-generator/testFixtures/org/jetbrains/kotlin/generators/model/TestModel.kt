/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.model

import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.utils.Printer
import org.junit.jupiter.api.Tag
import com.intellij.testFramework.TestDataPath

/**
 * Base class for test entities of the test generator, which are either test classes or test methods.
 *
 * @property name if a name of the class/method;
 * @property dataString contains string, which will be put into @[TestMetadata] annotation (if present).
 *   usually it contains path to the corresponding testdata;
 * @property tags contains a list of JUnit5 tags, which will be put into @[Tag] annotation (if present).
 */
sealed class TestEntityModel {
    abstract val name: String
    abstract val dataString: String?
    abstract val tags: List<String>
}

/**
 * The model which represents a test class. It could contain [innerTestClasses] and test [methods].
 *
 * @property dataPathRoot is a value which will be put into @[TestDataPath] annotation (if present).
 *   This annotation is used for IDE integration, and in combination with the @[TestMetadata] annotation
 *   allows IDE navigating from the test declaration to a corresponding testdata file.
 *
 * @property annotations is a list of annotations, which will be added to the generated test
 *   class in addition to the default set of annotations.
 *
 * Note that all kinds are generated in the same way regardless of the specific implementation.
 * @see org.jetbrains.kotlin.generators.dsl.junit4.TestGeneratorForJUnit4Instance.generateTestClass
 * @see org.jetbrains.kotlin.generators.dsl.junit5.TestGeneratorForJUnit5.TestGeneratorInstance.generateTestClass
 */
abstract class TestClassModel : TestEntityModel() {
    abstract val innerTestClasses: Collection<TestClassModel>
    abstract val methods: Collection<MethodModel<*>>
    abstract val isEmpty: Boolean
    abstract val dataPathRoot: String?
    abstract val annotations: Collection<AnnotationModel>

    val imports: Set<Class<*>>
        get() {
            return mutableSetOf<Class<*>>().also { allImports ->
                annotations.flatMapTo(allImports) { it.imports() }
                methods.flatMapTo(allImports) { it.imports() }
                innerTestClasses.flatMapTo(allImports) { it.imports }
            }
        }
}

/**
 * The model which represents a method inside the test class.
 *
 * @property generator defines how exactly the specific kind of the MethodModel should be generated
 */
abstract class MethodModel<M : MethodModel<M>> : TestEntityModel() {
    abstract val generator: MethodGenerator<M>

    /**
     * If false then no test annotations would be generated for the method (including `@Test`)
     */
    open val isTestMethod: Boolean get() = true
    open val shouldBeGeneratedForInnerTestClass: Boolean get() = true

    open fun imports(): Collection<Class<*>> = emptyList()

    fun generateBody(p: Printer) {
        @Suppress("UNCHECKED_CAST")
        generator.generateBody(this as M, p)
    }

    fun generateSignature(p: Printer) {
        @Suppress("UNCHECKED_CAST")
        generator.generateSignature(this as M, p)
    }
}
