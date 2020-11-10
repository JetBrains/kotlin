/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.model

interface TestEntityModel {
    val name: String
    val dataString: String?
}

interface ClassModel : TestEntityModel {
    val innerTestClasses: Collection<TestClassModel>
    val methods: Collection<MethodModel>
    val isEmpty: Boolean
    val dataPathRoot: String?
    val annotations: Collection<AnnotationModel>
    val imports: Set<Class<*>>
}

abstract class TestClassModel : ClassModel {
    override val imports: Set<Class<*>>
        get() {
            return mutableSetOf<Class<*>>().also { allImports ->
                annotations.mapTo(allImports) { it.annotation }
                methods.flatMapTo(allImports) { it.imports() }
                innerTestClasses.flatMapTo(allImports) { it.imports }
            }
        }
}

interface MethodModel : TestEntityModel {
    abstract class Kind

    val kind: Kind
    fun shouldBeGenerated(): Boolean = true
    fun imports(): Collection<Class<*>> = emptyList()
}
