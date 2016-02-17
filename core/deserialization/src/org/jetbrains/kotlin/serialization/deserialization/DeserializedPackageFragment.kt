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
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import java.io.InputStream
import javax.inject.Inject

abstract class DeserializedPackageFragment(
        fqName: FqName,
        protected val storageManager: StorageManager,
        module: ModuleDescriptor,
        protected val loadResource: (path: String) -> InputStream?
) : PackageFragmentDescriptorImpl(module, fqName) {
    // component dependency cycle
    @set:Inject
    lateinit var components: DeserializationComponents

    private val deserializedMemberScope by storageManager.createLazyValue {
        computeMemberScope()
    }

    abstract val classDataFinder: ClassDataFinder

    protected abstract fun computeMemberScope(): DeserializedPackageMemberScope

    override fun getMemberScope() = deserializedMemberScope

    internal fun hasTopLevelClass(name: Name): Boolean {
        return name in getMemberScope().classNames
    }

    protected fun loadResourceSure(path: String): InputStream =
            loadResource(path) ?: throw IllegalStateException("Resource not found in classpath: $path")
}
