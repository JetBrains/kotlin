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

package org.jetbrains.kotlin.builtins

import com.google.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PackageFragmentProviderImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.ClassData
import org.jetbrains.kotlin.serialization.NameSerializationUtil
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.builtins.BuiltInsProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.ClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeCapabilitiesDeserializer
import org.jetbrains.kotlin.serialization.deserialization.LocalClassResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.storage.StorageManager
import java.io.InputStream

public class BuiltinsPackageFragment(
        fqName: FqName,
        storageManager: StorageManager,
        module: ModuleDescriptor,
        flexibleTypeCapabilitiesDeserializer: FlexibleTypeCapabilitiesDeserializer,
        private val loadResource: (path: String) -> InputStream?
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val extensionRegistry: ExtensionRegistryLite

    ;{
        extensionRegistry = ExtensionRegistryLite.newInstance()
        BuiltInsProtoBuf.registerAllExtensions(extensionRegistry)
        extensionRegistry
    }

    private val nameResolver = NameSerializationUtil.deserializeNameResolver(
            getStream(BuiltInsSerializationUtil.getStringTableFilePath(fqName))
    )

    public val provider: PackageFragmentProvider = PackageFragmentProviderImpl(listOf(this))

    private val members: DeserializedPackageMemberScope = run {
        val proto = loadPackage()
        val localClassResolver = LocalClassResolverImpl()
        val components = DeserializationComponents(
                storageManager, module, BuiltInsClassDataFinder(),
                BuiltInsAnnotationAndConstantLoader(getContainingDeclaration()),
                provider, localClassResolver,
                flexibleTypeCapabilitiesDeserializer
        )
        localClassResolver.setDeserializationComponents(components)
        DeserializedPackageMemberScope(this, proto, nameResolver, components, { readClassNames(proto) })
    }

    private fun loadPackage(): ProtoBuf.Package {
        val stream = getStream(BuiltInsSerializationUtil.getPackageFilePath(fqName))
        return ProtoBuf.Package.parseFrom(stream, extensionRegistry)
    }

    private fun readClassNames(proto: ProtoBuf.Package): List<Name> {
        return proto.getExtension(BuiltInsProtoBuf.className)?.map { id -> nameResolver.getName(id) } ?: listOf()
    }

    override fun getMemberScope() = members

    private fun getStream(path: String): InputStream =
            loadResource(path) ?: throw IllegalStateException("Resource not found in classpath: $path")

    private inner class BuiltInsClassDataFinder : ClassDataFinder {
        override fun findClassData(classId: ClassId): ClassData? {
            val stream = loadResource(BuiltInsSerializationUtil.getClassMetadataPath(classId)) ?: return null

            val classProto = ProtoBuf.Class.parseFrom(stream, extensionRegistry)

            val expectedShortName = classId.getRelativeClassName().shortName()
            val actualShortName = nameResolver.getClassId(classProto.getFqName()).getRelativeClassName().shortName()
            if (!actualShortName.isSpecial() && actualShortName != expectedShortName) {
                // Workaround for case-insensitive file systems,
                // otherwise we'd find "Collection" for "collection" etc
                return null
            }

            return ClassData(nameResolver, classProto)
        }
    }
}
