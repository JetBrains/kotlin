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

package org.jetbrains.kotlin.javac

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName

class KotlinClassifiersCache {

    private val classifiers = hashMapOf<FqName, JavaClass>()

    fun getKotlinClassifier(fqName: FqName) = classifiers[fqName] ?: createClassifier(fqName)

    private fun createClassifier(fqName: FqName) = object : JavaClass {
        override val isAbstract: Boolean
            get() = false

        override val isStatic: Boolean
            get() = false

        override val isFinal: Boolean
            get() = false

        override val visibility: Visibility
            get() = Visibilities.PUBLIC

        override val typeParameters: List<JavaTypeParameter>
            get() = emptyList()

        override val fqName
            get() = fqName

        override val supertypes: Collection<JavaClassifierType>
            get() = emptyList()

        override val innerClasses: Collection<JavaClass>
            get() = emptyList()

        override val outerClass: JavaClass?
            get() = null

        override val isInterface: Boolean
            get() = false

        override val isAnnotationType: Boolean
            get() = false

        override val isEnum: Boolean
            get() = false

        override val lightClassOriginKind
            get() = LightClassOriginKind.SOURCE

        override val methods: Collection<JavaMethod>
            get() = emptyList()

        override val fields: Collection<JavaField>
            get() = emptyList()

        override val constructors: Collection<JavaConstructor>
            get() = emptyList()

        override val name
            get() = fqName.shortNameOrSpecial()

        override val annotations
            get() = emptyList<JavaAnnotation>()

        override val isDeprecatedInJavaDoc: Boolean
            get() = false

        override fun findAnnotation(fqName: FqName) = null

    }.apply { classifiers[fqName] = this }

}