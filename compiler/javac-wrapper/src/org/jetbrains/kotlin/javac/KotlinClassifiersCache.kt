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

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

class KotlinClassifiersCache(sourceFiles: Collection<KtFile>) {

    private val kotlinClasses: Map<FqName?, KtClassOrObject?> = sourceFiles.flatMap {
        (it.collectDescendantsOfType<KtClassOrObject>()
                .mapNotNull { it.fqName to it } + (it.javaFileFacadeFqName to null))
    }.toMap()

    private val classifiers = hashMapOf<FqName, JavaClass>()

    fun getKotlinClassifier(fqName: FqName) = classifiers[fqName] ?: createClassifier(fqName)

    private fun createClassifier(fqName: FqName): JavaClass? {
        if (!kotlinClasses.containsKey(fqName)) return null
        val kotlinClassifier = kotlinClasses[fqName]

        return MockKotlinClassifier(fqName,
                                    kotlinClassifier?.typeParameters?.isNotEmpty() ?: false)
                .apply { classifiers[fqName] = this }
    }

}

class MockKotlinClassifier(override val fqName: FqName,
                           val hasTypeParameters: Boolean) : JavaClass {

    override val isAbstract: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override val isStatic: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override val isFinal: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override val visibility: Visibility
        get() = throw UnsupportedOperationException("Should not be called")

    override val typeParameters: List<JavaTypeParameter>
        get() = throw UnsupportedOperationException("Should not be called")

    override val supertypes: Collection<JavaClassifierType>
        get() = throw UnsupportedOperationException("Should not be called")

    override val innerClasses: Collection<JavaClass>
        get() = throw UnsupportedOperationException("Should not be called")

    override val outerClass: JavaClass?
        get() = throw UnsupportedOperationException("Should not be called")

    override val isInterface: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override val isAnnotationType: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override val isEnum: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override val lightClassOriginKind
        get() = LightClassOriginKind.SOURCE

    override val methods: Collection<JavaMethod>
        get() = throw UnsupportedOperationException("Should not be called")

    override val fields: Collection<JavaField>
        get() = throw UnsupportedOperationException("Should not be called")

    override val constructors: Collection<JavaConstructor>
        get() = throw UnsupportedOperationException("Should not be called")

    override val name
        get() = fqName.shortNameOrSpecial()

    override val annotations
        get() = throw UnsupportedOperationException("Should not be called")

    override val isDeprecatedInJavaDoc: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override fun findAnnotation(fqName: FqName) = throw UnsupportedOperationException("Should not be called")

}