/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.types.lang

import org.jetbrains.jet.descriptors.serialization.*
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationAndConstantLoader
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider
import org.jetbrains.jet.lang.descriptors.PackageFragmentProviderImpl
import org.jetbrains.jet.lang.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.jet.lang.resolve.name.*
import org.jetbrains.jet.storage.StorageManager
import java.io.DataInputStream
import java.io.InputStream
import java.util.ArrayList
import org.jetbrains.jet.descriptors.serialization.context.DeserializationComponents

public class BuiltinsPackageFragment(
        fqName: FqName,
        storageManager: StorageManager,
        module: ModuleDescriptor,
        private val loadResource: (path: String) -> InputStream?
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val nameResolver = NameSerializationUtil.deserializeNameResolver(getStream(BuiltInsSerializationUtil.getNameTableFilePath(fqName)))

    public val provider: PackageFragmentProvider = PackageFragmentProviderImpl(listOf(this))

    private val members: DeserializedPackageMemberScope =
        DeserializedPackageMemberScope(
                this,
                loadPackage(),
                nameResolver,
                DeserializationComponents(
                        storageManager, module, BuiltInsClassDataFinder(), AnnotationAndConstantLoader.UNSUPPORTED, // TODO: support annotations
                        provider, FlexibleTypeCapabilitiesDeserializer.ThrowException
                ),
                { readClassNames() }
        )

    private fun loadPackage(): ProtoBuf.Package {
        val stream = getStream(BuiltInsSerializationUtil.getPackageFilePath(fqName))
        return ProtoBuf.Package.parseFrom(stream)
    }

    private fun readClassNames(): List<Name> {
        val stream = getStream(BuiltInsSerializationUtil.getClassNamesFilePath(fqName))
        return DataInputStream(stream).use { data ->
            val size = data.readInt()
            val result = ArrayList<Name>(size)
            size.times {
                result.add(nameResolver.getName(data.readInt()))
            }
            result
        }
    }

    override fun getMemberScope() = members

    private fun getStream(path: String): InputStream =
            loadResource(path) ?: throw IllegalStateException("Resource not found in classpath: $path")

    private inner class BuiltInsClassDataFinder : ClassDataFinder {
        override fun findClassData(classId: ClassId): ClassData? {
            val metadataPath = BuiltInsSerializationUtil.getClassMetadataPath(classId) ?: return null
            val stream = loadResource(metadataPath) ?: return null

            val classProto = ProtoBuf.Class.parseFrom(stream)

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
