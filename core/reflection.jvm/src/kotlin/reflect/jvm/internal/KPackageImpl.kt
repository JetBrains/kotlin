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

import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.structure.reflect.classId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.JetScope
import kotlin.jvm.internal.KotlinPackage
import kotlin.reflect.KCallable
import kotlin.reflect.KPackage

internal class KPackageImpl(override val jClass: Class<*>, val moduleName: String) : KDeclarationContainerImpl(), KPackage {
    val descriptor by ReflectProperties.lazySoft {
        val moduleData = moduleData
        moduleData.packageFacadeProvider.registerModule(moduleName)
        moduleData.module.getPackage(jClass.classId.packageFqName)
    }

    internal val scope: JetScope get() = descriptor.memberScope

    override val members: Collection<KCallable<*>>
        get() = getMembers(scope, declaredOnly = false, nonExtensions = true, extensions = true).toList()

    override val constructorDescriptors: Collection<ConstructorDescriptor>
        get() = emptyList()

    @Suppress("UNCHECKED_CAST")
    override fun getProperties(name: Name): Collection<PropertyDescriptor> =
            scope.getProperties(name, NoLookupLocation.FROM_REFLECTION) as Collection<PropertyDescriptor>

    override fun getFunctions(name: Name): Collection<FunctionDescriptor> =
            scope.getFunctions(name, NoLookupLocation.FROM_REFLECTION)

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
