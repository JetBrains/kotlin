/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.storage.StorageManager

abstract class DeserializedPackageFragmentImpl(
        fqName: FqName,
        storageManager: StorageManager,
        module: ModuleDescriptor,
        protected val proto: ProtoBuf.PackageFragment,
        private val containerSource: DeserializedContainerSource?
) : DeserializedPackageFragment(fqName, storageManager, module) {
    protected val nameResolver = NameResolverImpl(proto.strings, proto.qualifiedNames)

    override val classDataFinder = ProtoBasedClassDataFinder(proto, nameResolver) { containerSource ?: SourceElement.NO_SOURCE }

    override fun computeMemberScope() =
            DeserializedPackageMemberScope(
                    this, proto.`package`, nameResolver, containerSource, components,
                    classNames = {
                        classDataFinder.allClassIds.filter { classId ->
                            !classId.isNestedClass && classId !in ClassDeserializer.BLACK_LIST
                        }.map { it.shortClassName }
                    }
            )
}
