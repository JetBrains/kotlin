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

import org.jetbrains.kotlin.load.java.structure.reflect.classId
import org.jetbrains.kotlin.resolve.scopes.JetScope
import kotlin.jvm.internal.KotlinPackage
import kotlin.reflect.*

class KPackageImpl(override val jClass: Class<*>) : KCallableContainerImpl(), KPackage {
    val descriptor by ReflectProperties.lazySoft {
        val moduleData = jClass.getOrCreateModule()
        val fqName = jClass.classId.getPackageFqName()

        moduleData.module.getPackage(fqName) ?: throw KotlinReflectionInternalError("Package not resolved: $fqName")
    }

    override val scope: JetScope get() = descriptor.getMemberScope()

    fun topLevelVariable(name: String): KTopLevelVariable<*> {
        return KTopLevelVariableImpl(this, findPropertyDescriptor(name))
    }

    fun mutableTopLevelVariable(name: String): KMutableTopLevelVariable<*> {
        return KMutableTopLevelVariableImpl(this, findPropertyDescriptor(name))
    }

    fun <T> topLevelExtensionProperty(name: String, receiver: Class<T>): KTopLevelExtensionProperty<T, *> {
        return KTopLevelExtensionPropertyImpl(this, findPropertyDescriptor(name, receiver))
    }

    fun <T> mutableTopLevelExtensionProperty(name: String, receiver: Class<T>): KMutableTopLevelExtensionProperty<T, *> {
        return KMutableTopLevelExtensionPropertyImpl(this, findPropertyDescriptor(name, receiver))
    }

    override fun equals(other: Any?): Boolean =
            other is KPackageImpl && jClass == other.jClass

    override fun hashCode(): Int =
            jClass.hashCode()

    override fun toString(): String {
        val name = jClass.getName()
        return "package " + if (jClass.isAnnotationPresent(javaClass<KotlinPackage>())) {
            if (name == "_DefaultPackage") "<default>" else name.substringBeforeLast(".")
        }
        else name
    }
}
