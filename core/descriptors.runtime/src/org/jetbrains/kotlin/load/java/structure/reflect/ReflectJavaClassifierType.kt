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

public class ReflectJavaClassifierType(public override val type: Type) : ReflectJavaType(), JavaClassifierType {
    private val classifier: JavaClassifier = run {
        val type = type
        when (type) {
            is Class<*> -> ReflectJavaClass(type)
            is TypeVariable<*> -> ReflectJavaTypeParameter(type)
            is ParameterizedType -> ReflectJavaClass(type.getRawType() as Class<*>)
            else -> throw IllegalStateException("Not a classifier type (${type.javaClass}): $type")
        } : JavaClassifier
    }

    override fun getClassifier(): JavaClassifier = classifier

    override fun getSubstitutor(): JavaTypeSubstitutor = throw UnsupportedOperationException()

    override fun getSupertypes(): Collection<JavaClassifierType> = throw UnsupportedOperationException()

    override fun getPresentableText(): String = type.toString()

    override fun isRaw(): Boolean = with(type) { this is Class<*> && getTypeParameters().isNotEmpty() }

    override fun getTypeArguments(): List<JavaType> {
        return (type as? ParameterizedType)?.getActualTypeArguments()?.map { ReflectJavaType.create(it) } ?: listOf()
    }
}
