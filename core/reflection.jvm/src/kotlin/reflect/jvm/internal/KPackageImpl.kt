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
import org.jetbrains.kotlin.descriptors.runtime.components.ReflectKotlinClass
import org.jetbrains.kotlin.descriptors.runtime.structure.classId
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmNameResolver
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.MemberDeserializer
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.reflect.KCallable
import kotlin.reflect.jvm.internal.KDeclarationContainerImpl.MemberBelonginess.DECLARED

internal class KPackageImpl(
    override val jClass: Class<*>,
) : KDeclarationContainerImpl() {
    private inner class Data : KDeclarationContainerImpl.Data() {
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

        val metadata: Triple<JvmNameResolver, ProtoBuf.Package, JvmMetadataVersion>? by lazy(PUBLICATION) {
            kotlinClass?.classHeader?.let { header ->
                val data = header.data
                val strings = header.strings
                if (data != null && strings != null) {
                    val (nameResolver, proto) = JvmProtoBufUtil.readPackageDataFrom(data, strings)
                    Triple(nameResolver, proto, header.metadataVersion)
                } else null
            }
        }

        val members: Collection<KCallableImpl<*>> by ReflectProperties.lazySoft {
            getMembers(scope, DECLARED)
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

    override fun getLocalProperty(index: Int): PropertyDescriptor? {
        return data.value.metadata?.let { (nameResolver, packageProto, metadataVersion) ->
            packageProto.getExtensionOrNull(JvmProtoBuf.packageLocalVariable, index)?.let { proto ->
                deserializeToDescriptor(
                    jClass, proto, nameResolver, TypeTable(packageProto.typeTable), metadataVersion,
                    MemberDeserializer::loadProperty
                )
            }
        }
    }

    override fun equals(other: Any?): Boolean =
        other is KPackageImpl && jClass == other.jClass

    override fun hashCode(): Int =
        jClass.hashCode()

    override fun toString(): String =
        "file class ${jClass.classId.asSingleFqName()}"
}
