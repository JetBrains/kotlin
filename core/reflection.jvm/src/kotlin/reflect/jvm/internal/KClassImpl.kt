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

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.serialization.deserialization.findClassAcrossModuleDependencies
import kotlin.reflect.*

class KClassImpl<T>(override val jClass: Class<T>) : KCallableContainerImpl(), KClass<T> {
    // Don't use kotlin.properties.Delegates here because it's a Kotlin class which will invoke KClassImpl() in <clinit>,
    // resulting in infinite recursion

    val descriptor by ReflectProperties.lazySoft {
        val moduleData = jClass.getOrCreateModule()
        val classId = RuntimeTypeMapper.mapJvmClassToKotlinClassId(jClass)

        val descriptor =
                if (classId.isLocal()) moduleData.localClassResolver.resolveLocalClass(classId)
                else moduleData.module.findClassAcrossModuleDependencies(classId)

        descriptor ?: throw KotlinReflectionInternalError("Class not resolved: $jClass")
    }

    override val scope: JetScope get() = descriptor.getDefaultType().getMemberScope()

    override val simpleName: String? get() {
        val name = descriptor.getName()
        return if (name.isSpecial()) null else name.asString()
    }

    override fun getProperties(): Collection<KMemberProperty<T, *>> {
        return scope.getAllDescriptors().stream()
                .filterIsInstance<PropertyDescriptor>()
                .filter { descriptor ->
                    descriptor.getExtensionReceiverParameter() == null
                }
                .map { descriptor ->
                    if (descriptor.isVar()) KMutableMemberPropertyImpl<T, Any?>(this) { descriptor }
                    else KMemberPropertyImpl<T, Any?>(this) { descriptor }
                }
                .toList()
    }

    override fun getExtensionProperties(): Collection<KMemberExtensionProperty<T, *, *>> {
        return scope.getAllDescriptors().stream()
                .filterIsInstance<PropertyDescriptor>()
                .filter { descriptor ->
                    descriptor.getExtensionReceiverParameter() != null
                }
                .map { descriptor ->
                    if (descriptor.isVar()) KMutableMemberExtensionPropertyImpl<T, Any?, Any?>(this) { descriptor }
                    else KMemberExtensionPropertyImpl<T, Any?, Any?>(this) { descriptor }
                }
                .toList()
    }

    fun memberProperty(name: String): KMemberProperty<T, *> {
        return KMemberPropertyImpl<T, Any>(this, findPropertyDescriptor(name))
    }

    fun mutableMemberProperty(name: String): KMutableMemberProperty<T, *> {
        return KMutableMemberPropertyImpl<T, Any>(this, findPropertyDescriptor(name))
    }

    override fun equals(other: Any?): Boolean =
            other is KClassImpl<*> && jClass == other.jClass

    override fun hashCode(): Int =
            jClass.hashCode()

    override fun toString(): String =
            jClass.toString()
}
