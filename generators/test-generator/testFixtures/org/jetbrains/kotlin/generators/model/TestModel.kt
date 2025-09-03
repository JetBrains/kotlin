/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.model

sealed class TestEntityModel {
    abstract val name: String
    abstract val dataString: String?
    abstract val tags: List<String>
}

sealed class ClassModel : TestEntityModel() {
    abstract val innerTestClasses: Collection<TestClassModel>
    abstract val methods: Collection<MethodModel>
    abstract val isEmpty: Boolean
    abstract val dataPathRoot: String?
    abstract val annotations: Collection<AnnotationModel>
    abstract val imports: Set<Class<*>>
}

abstract class TestClassModel : ClassModel() {
    override val imports: Set<Class<*>>
        get() {
            return mutableSetOf<Class<*>>().also { allImports ->
                annotations.flatMapTo(allImports) { it.imports() }
                methods.flatMapTo(allImports) { it.imports() }
                innerTestClasses.flatMapTo(allImports) { it.imports }
            }
        }
}

abstract class MethodModel : TestEntityModel() {
    abstract class Kind

    abstract val kind: Kind
    open fun isTestMethod(): Boolean = true
    open fun shouldBeGeneratedForInnerTestClass(): Boolean = true
    open fun imports(): Collection<Class<*>> = emptyList()
}
