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

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.builtins.BuiltInsProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion
import org.jetbrains.kotlin.serialization.deserialization.DeserializedPackageFragment
import org.jetbrains.kotlin.serialization.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.storage.StorageManager
import java.io.DataInputStream
import java.io.InputStream

class BuiltinsPackageFragment(
        fqName: FqName,
        storageManager: StorageManager,
        module: ModuleDescriptor,
        loadResource: (path: String) -> InputStream?
) : DeserializedPackageFragment(fqName, storageManager, module, BuiltInsSerializedResourcePaths, loadResource) {
    val builtinsMessage = loadResource(BuiltInsSerializedResourcePaths.getBuiltInsFilePath(fqName))?.let { stream ->
        val dataInput = DataInputStream(stream)
        val version = BuiltInsBinaryVersion.create((1..dataInput.readInt()).map { dataInput.readInt() }.toIntArray())

        if (!version.isCompatibleTo(BuiltinsPackageFragment.VERSION)) {
            // TODO: report a proper diagnostic
            throw UnsupportedOperationException(
                    "Kotlin built-in definition format version is not supported: " +
                    "expected ${BuiltinsPackageFragment.VERSION}, actual $version. " +
                    "Please update Kotlin"
            )
        }

        BuiltInsProtoBuf.BuiltIns.parseFrom(stream, BuiltInsSerializedResourcePaths.extensionRegistry)
    }

    override val nameResolver =
            builtinsMessage?.let { NameResolverImpl(it.strings, it.qualifiedNames) }
            ?: NameResolverImpl.read(loadResourceSure(serializedResourcePaths.getStringTableFilePath(fqName)))

    override val classIdToProto = builtinsMessage?.let { builtins ->
        builtins.classList.toMapBy { klass ->
            nameResolver.getClassId(klass.fqName)
        }
    }

    override fun computeMemberScope() = builtinsMessage?.let { builtins ->
        DeserializedPackageMemberScope(
                this, builtins.`package`, nameResolver, packagePartSource = null, components = components,
                classNames = { classIdToProto!!.keys.filter { classId -> !classId.isNestedClass }.map { it.shortClassName } }
        )
    } ?: super.computeMemberScope()

    override fun loadClassNames(packageProto: ProtoBuf.Package): Collection<Name> {
        return packageProto.getExtension(BuiltInsProtoBuf.className)?.map { id -> nameResolver.getName(id) } ?: listOf()
    }

    companion object {
        // Advance this version when the common or builtins-specific binary metadata format is changed
        val VERSION = BuiltInsBinaryVersion.create(1, 0, 0)
    }
}
