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
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberScope
import org.jetbrains.kotlin.storage.StorageManager

abstract class DeserializedPackageFragment(
    fqName: FqName,
    protected val storageManager: StorageManager,
    module: ModuleDescriptor
) : PackageFragmentDescriptorImpl(module, fqName) {

    abstract fun initialize(components: DeserializationComponents)

    abstract val classDataFinder: ClassDataFinder

    open fun hasTopLevelClass(name: Name): Boolean {
        val scope = getMemberScope()
        return scope is DeserializedMemberScope && name in scope.classNames
    }
}
