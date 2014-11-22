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
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationLoader
import org.jetbrains.jet.descriptors.serialization.descriptors.ConstantLoader
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider
import org.jetbrains.jet.lang.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.jet.lang.resolve.name.ClassId
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.storage.StorageManager
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import org.jetbrains.jet.descriptors.serialization.context.DeserializationComponents

class BuiltinsPackageFragment(storageManager: StorageManager, module: ModuleDescriptor)
  : PackageFragmentDescriptorImpl(module, KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME) {

    private val nameResolver = NameSerializationUtil.deserializeNameResolver(getStream(BuiltInsSerializationUtil.getNameTableFilePath(fqName)))

    public val provider: PackageFragmentProvider = BuiltinsPackageFragmentProvider()

    private val members: DeserializedPackageMemberScope =
        DeserializedPackageMemberScope(
                this,
                loadPackage(),
                nameResolver,
                DeserializationComponents(
                        storageManager, module, BuiltInsClassDataFinder(), AnnotationLoader.UNSUPPORTED, // TODO: support annotations
                        ConstantLoader.UNSUPPORTED, provider, FlexibleTypeCapabilitiesDeserializer.ThrowException
                ),
                { readClassNames() }
        )

    private fun loadPackage(): ProtoBuf.Package {
        val packageFilePath = BuiltInsSerializationUtil.getPackageFilePath(fqName)
        val stream = getStream(packageFilePath)
        try {
            return ProtoBuf.Package.parseFrom(stream)
        }
        catch (e: IOException) {
            throw IllegalStateException(e)
        }

    }

    private fun readClassNames(): List<Name> {
        val `in` = getStream(BuiltInsSerializationUtil.getClassNamesFilePath(fqName))
        val data = DataInputStream(`in`)
        try {
            val size = data.readInt()
            val result = ArrayList<Name>(size)
            for (i in 0..size - 1) {
                result.add(nameResolver.getName(data.readInt()))
            }
            return result
        }
        finally {
            data.close()
        }
    }

    override fun getMemberScope() = members

    private fun getStream(path: String) = getStreamNullable(path) ?: throw IllegalStateException("Resource not found in classpath: " + path)

    private fun getStreamNullable(path: String): InputStream? = javaClass<KotlinBuiltIns>().getClassLoader().getResourceAsStream(path)

    private inner class BuiltinsPackageFragmentProvider : PackageFragmentProvider {
        override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor>
                = if (KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME == fqName) listOf(this@BuiltinsPackageFragment) else listOf()

        override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName>
                = if (fqName.isRoot()) setOf(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME) else listOf()
    }

    private inner class BuiltInsClassDataFinder : ClassDataFinder {
        override fun findClassData(classId: ClassId): ClassData? {
            val metadataPath = BuiltInsSerializationUtil.getClassMetadataPath(classId) ?: return null
            val stream = getStreamNullable(metadataPath) ?: return null

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
