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

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.builtins.BuiltinsPackageFragment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.SerializedResourcePaths
import org.jetbrains.kotlin.serialization.builtins.BuiltInsProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import java.io.DataInputStream
import java.io.InputStream
import javax.inject.Inject

public abstract class DeserializedPackageFragment(
        fqName: FqName,
        protected val storageManager: StorageManager,
        module: ModuleDescriptor,
        protected val serializedResourcePaths: SerializedResourcePaths,
        private val loadResource: (path: String) -> InputStream?
) : PackageFragmentDescriptorImpl(module, fqName) {

    val builtinsMessage = serializedResourcePaths.getBuiltInsFilePath(fqName)?.let(loadResource)?.let { stream ->
        val dataInput = DataInputStream(stream)
        val version = BinaryVersion.create((1..dataInput.readInt()).map { dataInput.readInt() }.toIntArray())

        if (!version.isCompatibleTo(BuiltinsPackageFragment.VERSION)) {
            // TODO: report a proper diagnostic
            throw UnsupportedOperationException(
                    "Kotlin built-in definition format version is not supported: " +
                    "expected ${BuiltinsPackageFragment.VERSION}, actual $version. " +
                    "Please update Kotlin"
            )
        }

        BuiltInsProtoBuf.BuiltIns.parseFrom(stream, serializedResourcePaths.extensionRegistry)
    }

    val nameResolver =
            builtinsMessage?.let { NameResolverImpl(it.strings, it.qualifiedNames) } ?:
            NameResolverImpl.read(loadResourceSure(serializedResourcePaths.getStringTableFilePath(fqName)))

    val classIdToProto = builtinsMessage?.let { builtins ->
        builtins.classList.toMapBy { klass ->
            nameResolver.getClassId(klass.fqName)
        }
    }

    // component dependency cycle
    @set:Inject
    lateinit var components: DeserializationComponents

    internal val deserializedMemberScope by storageManager.createLazyValue {
        builtinsMessage?.let { builtins ->
            DeserializedPackageMemberScope(
                    this, builtins.`package`, nameResolver, packagePartSource = null, components = components,
                    classNames = { classIdToProto!!.keys.filter { classId -> !classId.isNestedClass }.map { it.shortClassName } }
            )
        } ?: run {
            val packageStream = loadResourceSure(serializedResourcePaths.getPackageFilePath(fqName))
            val packageProto = ProtoBuf.Package.parseFrom(packageStream, serializedResourcePaths.extensionRegistry)
            DeserializedPackageMemberScope(
                    this, packageProto, nameResolver, packagePartSource = null, components = components,
                    classNames = { loadClassNames(packageProto) }
            )
        }
    }

    override fun getMemberScope() = deserializedMemberScope

    internal fun hasTopLevelClass(name: Name): Boolean {
        return name in getMemberScope().classNames
    }

    protected abstract fun loadClassNames(packageProto: ProtoBuf.Package): Collection<Name>

    protected fun loadResourceSure(path: String): InputStream =
            loadResource(path) ?: throw IllegalStateException("Resource not found in classpath: $path")
}
