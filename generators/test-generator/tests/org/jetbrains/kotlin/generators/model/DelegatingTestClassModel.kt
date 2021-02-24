/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.generators.model

open class DelegatingTestClassModel(private val delegate: TestClassModel) : TestClassModel() {
    override val name: String
        get() = delegate.name

    override val innerTestClasses: Collection<TestClassModel>
        get() = delegate.innerTestClasses

    override val methods: Collection<MethodModel>
        get() = delegate.methods

    override val isEmpty: Boolean
        get() = delegate.isEmpty

    override val dataPathRoot: String?
        get() = delegate.dataPathRoot

    override val dataString: String?
        get() = delegate.dataString

    override val annotations: Collection<AnnotationModel>
        get() = delegate.annotations
}
