/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.descriptors.runtime.components

import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.sources.JavaSourceElementFactory
import org.jetbrains.kotlin.load.java.structure.JavaElement
import org.jetbrains.kotlin.descriptors.runtime.structure.ReflectJavaElement

object RuntimeSourceElementFactory : JavaSourceElementFactory {
    class RuntimeSourceElement(override val javaElement: ReflectJavaElement) : JavaSourceElement {
        override fun toString() = this::class.java.name + ": " + javaElement.toString()
        override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE
    }

    override fun source(javaElement: JavaElement): JavaSourceElement =
        RuntimeSourceElement(javaElement as ReflectJavaElement)
}
