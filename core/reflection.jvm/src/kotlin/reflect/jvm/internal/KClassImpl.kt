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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.serialization.deserialization.findClassAcrossModuleDependencies
import kotlin.jvm.internal.KotlinClass
import kotlin.reflect.KClass
import kotlin.reflect.KMemberProperty
import kotlin.reflect.KMutableMemberProperty
import kotlin.reflect.KotlinReflectionInternalError

enum class KClassOrigin {
    BUILT_IN
    KOTLIN
    FOREIGN
}

class KClassImpl<T>(override val jClass: Class<T>) : KCallableContainerImpl(), KClass<T> {
    // Don't use kotlin.properties.Delegates here because it's a Kotlin class which will invoke KClassImpl() in <clinit>,
    // resulting in infinite recursion

    val descriptor by ReflectProperties.lazySoft {(): ClassDescriptor ->
        val moduleData = jClass.getOrCreateModule()
        val classId = RuntimeTypeMapper.mapJvmClassToKotlinClassId(jClass)

        val descriptor =
                if (classId.isLocal()) moduleData.localClassResolver.resolveLocalClass(classId)
                else moduleData.module.findClassAcrossModuleDependencies(classId)

        descriptor ?: throw KotlinReflectionInternalError("Class not resolved: $jClass")
    }

    override val scope: JetScope get() = descriptor.getDefaultType().getMemberScope()

    private val origin by ReflectProperties.lazy {(): KClassOrigin ->
        if (jClass.isAnnotationPresent(javaClass<KotlinClass>())) {
            KClassOrigin.KOTLIN
        }
        else {
            KClassOrigin.FOREIGN
            // TODO: built-in classes
        }
    }

    fun memberProperty(name: String): KMemberProperty<T, *> {
        val computeDescriptor = findPropertyDescriptor(name)

        if (origin === KClassOrigin.KOTLIN) {
            return KMemberPropertyImpl<T, Any>(this, computeDescriptor)
        }
        else {
            return KForeignMemberProperty<T, Any>(name, this)
        }
    }

    fun mutableMemberProperty(name: String): KMutableMemberProperty<T, *> {
        val computeDescriptor = findPropertyDescriptor(name)

        if (origin === KClassOrigin.KOTLIN) {
            return KMutableMemberPropertyImpl<T, Any>(this, computeDescriptor)
        }
        else {
            return KMutableForeignMemberProperty<T, Any>(name, this)
        }
    }

    override fun equals(other: Any?): Boolean =
            other is KClassImpl<*> && jClass == other.jClass

    override fun hashCode(): Int =
            jClass.hashCode()

    override fun toString(): String =
            jClass.toString()
}
