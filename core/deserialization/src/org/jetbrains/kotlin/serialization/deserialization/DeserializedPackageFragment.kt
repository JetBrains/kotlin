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

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.SerializedResourcePaths
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.get
import java.io.InputStream
import javax.inject.Inject
import kotlin.properties.Delegates

public abstract class DeserializedPackageFragment(
        fqName: FqName,
        protected val storageManager: StorageManager,
        module: ModuleDescriptor,
        protected val serializedResourcePaths: SerializedResourcePaths,
        private val loadResource: (path: String) -> InputStream?
) : PackageFragmentDescriptorImpl(module, fqName) {

    val nameResolver = NameResolverImpl.read(
            loadResource(serializedResourcePaths.getStringTableFilePath(fqName))
            ?: loadResourceSure(serializedResourcePaths.fallbackPaths.getStringTableFilePath(fqName))
    )

    protected var components: DeserializationComponents by Delegates.notNull()

    // component dependency cycle
    @Inject
    public fun setDeserializationComponents(components: DeserializationComponents) {
        this.components = components
    }

    internal val deserializedMemberScope by storageManager.createLazyValue {
        val packageStream = loadResourceSure(serializedResourcePaths.getPackageFilePath(fqName))
        val packageProto = ProtoBuf.Package.parseFrom(packageStream, serializedResourcePaths.extensionRegistry)
        DeserializedPackageMemberScope(this, packageProto, nameResolver, components, classNames = { loadClassNames(packageProto) })
    }

    override fun getMemberScope() = deserializedMemberScope

    internal fun hasTopLevelClass(name: Name): Boolean {
        return name in getMemberScope().classNames
    }

    protected abstract fun loadClassNames(packageProto: ProtoBuf.Package): Collection<Name>

    protected fun loadResourceSure(path: String): InputStream =
            loadResource(path) ?: throw IllegalStateException("Resource not found in classpath: $path")
}
