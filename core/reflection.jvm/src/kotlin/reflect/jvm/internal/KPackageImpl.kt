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

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.runtime.components.ReflectKotlinClass
import org.jetbrains.kotlin.descriptors.runtime.structure.classId
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.metadata.KmPackage
import kotlin.metadata.KmProperty
import kotlin.metadata.internal.toKmPackage
import kotlin.metadata.jvm.localDelegatedProperties
import kotlin.reflect.KCallable

internal class KPackageImpl(
    override val jClass: Class<*>,
) : KDeclarationContainerImpl() {
    private inner class Data : KDeclarationContainerImpl.Data() {
        val kmPackage: KmPackage? by lazy(PUBLICATION) {
            val scope = scope as? DeserializedPackageMemberScope ?: return@lazy null
            scope.proto.toKmPackage(scope.c.nameResolver)
        }

        private val kotlinClass: ReflectKotlinClass? by ReflectProperties.lazySoft {
            ReflectKotlinClass.create(jClass)
        }

        val scope: MemberScope by ReflectProperties.lazySoft {
            val klass = kotlinClass

            if (klass != null)
                moduleData.packagePartScopeCache.getPackagePartScope(klass)
            else MemberScope.Empty
        }

        val multifileFacade: Class<*>? by lazy(PUBLICATION) {
            val facadeName = kotlinClass?.classHeader?.multifileClassName
            // We need to check isNotEmpty because this is the value read from the annotation which cannot be null.
            // The default value for 'xs' is empty string, as declared in kotlin.Metadata
            if (facadeName != null && facadeName.isNotEmpty())
                jClass.classLoader.loadClass(facadeName.replace('/', '.'))
            else null
        }

        val members: Collection<DescriptorKCallable<*>> by ReflectProperties.lazySoft {
            val visitor = object : CreateKCallableVisitor(this@KPackageImpl) {
                override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: Unit): DescriptorKCallable<*> =
                    throw IllegalStateException("No constructors should appear here: $descriptor")
            }
            scope.getContributedDescriptors().mapNotNull { descriptor ->
                if (descriptor is CallableMemberDescriptor) descriptor.accept(visitor, Unit) else null
            }.toList()
        }
    }

    private val data = lazy(PUBLICATION) { Data() }

    override val methodOwner: Class<*> get() = data.value.multifileFacade ?: jClass

    private val scope: MemberScope get() = data.value.scope

    override val members: Collection<KCallable<*>> get() = data.value.members

    override val constructorDescriptors: Collection<ConstructorDescriptor>
        get() = emptyList()

    override fun getProperties(name: Name): Collection<PropertyDescriptor> =
        scope.getContributedVariables(name, NoLookupLocation.FROM_REFLECTION)

    override fun getFunctions(name: Name): Collection<FunctionDescriptor> =
        scope.getContributedFunctions(name, NoLookupLocation.FROM_REFLECTION)

    override fun getLocalPropertyDescriptor(index: Int): PropertyDescriptor? {
        // According to how it's generated in the codegen, containing class of a local delegated property is always either a single file
        // facade, or a multifile part, but never multifile facade. This means that `scope` is always `DeserializedPackageMemberScope`
        // (not `ChainedMemberScope` with several deserialized scopes inside, as is for multifile facades).
        val scope = scope as? DeserializedPackageMemberScope ?: return null
        val packageProto = scope.proto
        return packageProto.getExtensionOrNull(JvmProtoBuf.packageLocalVariable, index)?.let { proto ->
            deserializeToDescriptor(
                jClass, proto, scope.c.nameResolver, TypeTable(packageProto.typeTable), scope.c.metadataVersion
            ) { proto -> loadProperty(proto, loadAnnotationsFromMetadata = true) }
        }
    }

    override fun getLocalPropertyMetadata(index: Int): KmProperty? =
        data.value.kmPackage?.localDelegatedProperties?.getOrNull(index)

    override fun equals(other: Any?): Boolean =
        other is KPackageImpl && jClass == other.jClass

    override fun hashCode(): Int =
        jClass.hashCode()

    override fun toString(): String =
        "file class ${jClass.classId.asSingleFqName()}"
}
