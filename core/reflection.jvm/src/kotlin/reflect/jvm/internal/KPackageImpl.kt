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
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.load.java.structure.reflect.classId
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryPackageSourceElement
import org.jetbrains.kotlin.load.kotlin.reflect.ReflectKotlinClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import kotlin.reflect.KCallable

internal class KPackageImpl(override val jClass: Class<*>, val moduleName: String) : KDeclarationContainerImpl() {
    private val descriptor = ReflectProperties.lazySoft {
        with(moduleData) {
            packageFacadeProvider.registerModule(moduleName)
            module.getPackage(jClass.classId.packageFqName)
        }
    }

    override val methodOwner: Class<*> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val facadeName = ReflectKotlinClass.create(jClass)?.classHeader?.multifileClassName
        // We need to check isNotEmpty because this is the value read from the annotation which cannot be null.
        // The default value for 'xs' is empty string, as declared in kotlin.Metadata
        // TODO: do not read ReflectKotlinClass multiple times, obtain facade name from descriptor
        if (facadeName != null && facadeName.isNotEmpty()) {
            jClass.classLoader.loadClass(facadeName.replace('/', '.'))
        }
        else {
            jClass
        }
    }

    internal val scope: MemberScope get() = descriptor().memberScope

    override val members: Collection<KCallable<*>>
        get() = getMembers(scope, declaredOnly = false, nonExtensions = true, extensions = true).filter { member ->
            val callableDescriptor = member.descriptor as DeserializedCallableMemberDescriptor
            val packageFragment = callableDescriptor.containingDeclaration as PackageFragmentDescriptor
            val source = (packageFragment as? LazyJavaPackageFragment)?.source as? KotlinJvmBinaryPackageSourceElement
            (source?.getContainingBinaryClass(callableDescriptor) as? ReflectKotlinClass)?.klass == jClass
        }.toList()

    override val constructorDescriptors: Collection<ConstructorDescriptor>
        get() = emptyList()

    override fun getProperties(name: Name): Collection<PropertyDescriptor> =
            scope.getContributedVariables(name, NoLookupLocation.FROM_REFLECTION)

    override fun getFunctions(name: Name): Collection<FunctionDescriptor> =
            scope.getContributedFunctions(name, NoLookupLocation.FROM_REFLECTION)

    override fun equals(other: Any?): Boolean =
            other is KPackageImpl && jClass == other.jClass

    override fun hashCode(): Int =
            jClass.hashCode()

    override fun toString(): String {
        val fqName = jClass.classId.packageFqName
        return "package " + (if (fqName.isRoot) "<default>" else fqName.asString())
    }
}
