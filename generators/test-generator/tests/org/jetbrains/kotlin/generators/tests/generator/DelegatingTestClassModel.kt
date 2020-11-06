/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin.generators.tests.generator

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
