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

package org.jetbrains.kotlin.load.java.structure.reflect

import org.jetbrains.kotlin.load.java.structure.JavaClassifier
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.load.java.structure.JavaTypeSubstitutor
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable

public class ReflectJavaClassifierType(val classifierType: Type) : ReflectJavaType(), JavaClassifierType {
    override fun getClassifier(): JavaClassifier? {
        return when (classifierType) {
            is Class<*> -> ReflectJavaClass(classifierType)
            is TypeVariable<*> -> ReflectJavaTypeParameter(classifierType)
            // TODO
            is ParameterizedType -> ReflectJavaClass(classifierType.getRawType() as Class<*>)
            else -> throw UnsupportedOperationException("Unsupported type (${classifierType.javaClass}): $classifierType")
        }
    }

    override fun getSubstitutor(): JavaTypeSubstitutor = throw UnsupportedOperationException()

    override fun getSupertypes(): Collection<JavaClassifierType> = throw UnsupportedOperationException()

    override fun getPresentableText(): String = classifierType.toString()

    override fun isRaw(): Boolean = with(classifierType) { this is Class<*> && getTypeParameters().isNotEmpty() }

    override fun getTypeArguments(): List<JavaType> {
        // TODO
        return listOf()
    }
}
