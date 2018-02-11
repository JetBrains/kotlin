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

import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

abstract class ReflectJavaAnnotationArgument(
        override val name: Name?
) : JavaAnnotationArgument {
    companion object Factory {
        fun create(value: Any, name: Name?): ReflectJavaAnnotationArgument {
            return when {
                value::class.java.isEnumClassOrSpecializedEnumEntryClass() -> ReflectJavaEnumValueAnnotationArgument(name, value as Enum<*>)
                value is Annotation -> ReflectJavaAnnotationAsAnnotationArgument(name, value)
                value is Array<*> -> ReflectJavaArrayAnnotationArgument(name, value)
                value is Class<*> -> ReflectJavaClassObjectAnnotationArgument(name, value)
                else -> ReflectJavaLiteralAnnotationArgument(name, value)
            }
        }
    }
}

class ReflectJavaLiteralAnnotationArgument(
        name: Name?,
        override val value: Any
) : ReflectJavaAnnotationArgument(name), JavaLiteralAnnotationArgument

class ReflectJavaArrayAnnotationArgument(
        name: Name?,
        private val values: Array<*>
) : ReflectJavaAnnotationArgument(name), JavaArrayAnnotationArgument {
    override fun getElements() = values.map { ReflectJavaAnnotationArgument.create(it!!, null) }
}

class ReflectJavaEnumValueAnnotationArgument(
        name: Name?,
        private val value: Enum<*>
) : ReflectJavaAnnotationArgument(name), JavaEnumValueAnnotationArgument {
    override val enumClassId: ClassId?
        get() {
            val clazz = value::class.java
            val enumClass = if (clazz.isEnum) clazz else clazz.enclosingClass
            return enumClass.classId
        }

    override val entryName: Name?
        get() = Name.identifier(value.name)
}

class ReflectJavaClassObjectAnnotationArgument(
        name: Name?,
        private val klass: Class<*>
) : ReflectJavaAnnotationArgument(name), JavaClassObjectAnnotationArgument {
    override fun getReferencedType(): JavaType = ReflectJavaType.create(klass)
}

class ReflectJavaAnnotationAsAnnotationArgument(
        name: Name?,
        private val annotation: Annotation
) : ReflectJavaAnnotationArgument(name), JavaAnnotationAsAnnotationArgument {
    override fun getAnnotation(): JavaAnnotation = ReflectJavaAnnotation(annotation)
}
