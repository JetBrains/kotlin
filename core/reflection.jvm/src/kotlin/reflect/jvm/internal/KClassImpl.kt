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
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.serialization.deserialization.findClassAcrossModuleDependencies
import kotlin.reflect.*

class KClassImpl<T>(override val jClass: Class<T>) : KCallableContainerImpl(), KClass<T> {
    // Don't use kotlin.properties.Delegates here because it's a Kotlin class which will invoke KClassImpl() in <clinit>,
    // resulting in infinite recursion

    val descriptor by ReflectProperties.lazySoft {
        val classId = classId

        val descriptor =
                if (classId.isLocal()) moduleData.localClassResolver.resolveLocalClass(classId)
                else moduleData.module.findClassAcrossModuleDependencies(classId)

        descriptor ?: throw KotlinReflectionInternalError("Class not resolved: $jClass")
    }

    private val classId: ClassId get() = RuntimeTypeMapper.mapJvmClassToKotlinClassId(jClass)

    override val scope: JetScope get() = descriptor.getDefaultType().getMemberScope()

    override val simpleName: String? get() {
        val classId = classId
        return when {
            jClass.isAnonymousClass() -> null
            classId.isLocal() -> calculateLocalClassName(jClass)
            else -> classId.getShortClassName().asString()
        }
    }

    private fun calculateLocalClassName(jClass: Class<*>): String {
        val name = jClass.getSimpleName()
        jClass.getEnclosingMethod()?.let { method ->
            return name.substringAfter(method.getName() + "$")
        }
        jClass.getEnclosingConstructor()?.let { constructor ->
            return name.substringAfter(constructor.getName() + "$")
        }
        return name.substringAfter('$')
    }

    override val properties: Collection<KMemberProperty<T, *>>
            get() = getProperties(declared = false)

    override val extensionProperties: Collection<KMemberExtensionProperty<T, *, *>>
            get() = getExtensionProperties(declared = false)

    fun getProperties(declared: Boolean): Collection<KMemberProperty<T, *>> =
            getProperties(extension = false, declared = declared) { descriptor ->
                if (descriptor.isVar()) KMutableMemberPropertyImpl<T, Any?>(this, descriptor)
                else KMemberPropertyImpl<T, Any?>(this, descriptor)
            }

    fun getExtensionProperties(declared: Boolean): Collection<KMemberExtensionProperty<T, *, *>> =
            getProperties(extension = true, declared = declared) { descriptor ->
                if (descriptor.isVar()) KMutableMemberExtensionPropertyImpl<T, Any?, Any?>(this, descriptor)
                else KMemberExtensionPropertyImpl<T, Any?, Any?>(this, descriptor)
            }

    private fun <P : KProperty<*>> getProperties(extension: Boolean, declared: Boolean, create: (PropertyDescriptor) -> P): Collection<P> =
            scope.getAllDescriptors().sequence()
                    .filterIsInstance<PropertyDescriptor>()
                    .filter { descriptor ->
                        (descriptor.getExtensionReceiverParameter() != null) == extension &&
                        (descriptor.getKind().isReal() || !declared) &&
                        descriptor.getVisibility() != Visibilities.INVISIBLE_FAKE
                    }
                    .map(create)
                    .toList()

    fun memberProperty(name: String): KMemberProperty<T, *> =
            KMemberPropertyImpl<T, Any>(this, name)

    fun mutableMemberProperty(name: String): KMutableMemberProperty<T, *> =
            KMutableMemberPropertyImpl<T, Any>(this, name)

    override fun equals(other: Any?): Boolean =
            other is KClassImpl<*> && jClass == other.jClass

    override fun hashCode(): Int =
            jClass.hashCode()

    override fun toString(): String {
        return "class " + classId.let { classId ->
            val packageFqName = classId.getPackageFqName()
            val packagePrefix = if (packageFqName.isRoot()) "" else packageFqName.asString() + "."
            val classSuffix = classId.getRelativeClassName().asString().replace('.', '$')
            packagePrefix + classSuffix
        }
    }
}
