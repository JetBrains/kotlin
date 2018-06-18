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

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.library.KonanLibraryReader
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.metadata.KonanLinkData
import org.jetbrains.kotlin.serialization.deserialization.DeserializedPackageFragment
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.storage.StorageManager

class KonanPackageFragment(
        val fqNameString: String,
        val reader: KonanLibraryReader,
        storageManager: StorageManager, module: ModuleDescriptor
) : DeserializedPackageFragment(FqName(fqNameString), storageManager, module) {

    lateinit var components: DeserializationComponents

    override fun initialize(components: DeserializationComponents) {
        this.components = components
    }

    // The proto field is lazy so that we can load only needed
    // packages from the library.
    private val protoForNames: KonanLinkData.LinkDataPackageFragment by lazy {
        parsePackageFragment(reader.packageMetadata(fqNameString))
    }

    val proto: KonanLinkData.LinkDataPackageFragment get() = protoForNames.also {
        reader.markPackageAccessed(fqNameString)
    }

    private val nameResolver by lazy {
        NameResolverImpl(protoForNames.getStringTable(), protoForNames.getNameTable())
    }

    override val classDataFinder by lazy {
        KonanClassDataFinder(proto, nameResolver)
    }

    private val memberScope_ by lazy {
        DeserializedPackageMemberScope(this, proto.getPackage(),
                nameResolver, /* containerSource = */ null,
                components, { loadClassNames() })
    }

    override fun getMemberScope(): DeserializedPackageMemberScope = memberScope_

    private val classifierNames by lazy {
        val result = mutableSetOf<Name>()
        result.addAll(loadClassNames())
        protoForNames.getPackage().typeAliasList.mapTo(result) { nameResolver.getName(it.name) }
        result
    }

    fun hasTopLevelClassifier(name: Name): Boolean = name in classifierNames

    private fun loadClassNames(): Collection<Name> {

        val classNameList = protoForNames.getClasses().getClassNameList()

        val names = classNameList.mapNotNull { 
            val classId = nameResolver.getClassId(it)
            val shortName = classId.getShortClassName()
            if (!classId.isNestedClass) shortName else null
       }

       return names
    }
}

