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

package kotlin.reflect.jvm.internal.structure

import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClassifier
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.name.FqName
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable

class ReflectJavaClassifierType(public override val reflectType: Type) : ReflectJavaType(), JavaClassifierType {
    override val classifier: JavaClassifier = run {
        val type = reflectType
        val classifier: JavaClassifier = when (type) {
            is Class<*> -> ReflectJavaClass(type)
            is TypeVariable<*> -> ReflectJavaTypeParameter(type)
            is ParameterizedType -> ReflectJavaClass(type.rawType as Class<*>)
            else -> throw IllegalStateException("Not a classifier type (${type::class.java}): $type")
        }
        classifier
    }

    override val classifierQualifiedName: String
        get() = throw UnsupportedOperationException("Type not found: $reflectType")

    override val presentableText: String
        get() = reflectType.toString()

    override val isRaw: Boolean
        get() = with(reflectType) { this is Class<*> && getTypeParameters().isNotEmpty() }

    override val typeArguments: List<JavaType>
        get() = reflectType.parameterizedTypeArguments.map(Factory::create)

    override val annotations: Collection<JavaAnnotation>
        get() {
            return emptyList() // TODO
        }

    override fun findAnnotation(fqName: FqName): JavaAnnotation? {
        return null // TODO
    }

    override val isDeprecatedInJavaDoc: Boolean
        get() = false
}
