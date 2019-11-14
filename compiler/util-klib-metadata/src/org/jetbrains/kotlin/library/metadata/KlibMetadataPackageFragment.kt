/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.DeserializedPackageFragment
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.storage.StorageManager

class KlibMetadataDeserializedPackageFragment(
    fqName: FqName,
    private val library: KotlinLibrary,
    private val packageAccessHandler: PackageAccessHandler?,
    storageManager: StorageManager,
    module: ModuleDescriptor,
    private val partName: String
) : KlibMetadataPackageFragment(fqName, storageManager, module) {

    // The proto field is lazy so that we can load only needed
    // packages from the library.
    override val protoForNames: ProtoBuf.PackageFragment by lazy {
        (packageAccessHandler ?: SimplePackageAccessHandler).loadPackageFragment(library, fqName.asString(), partName)
    }

    override val proto: ProtoBuf.PackageFragment
        get() {
            packageAccessHandler?.markNeededForLink(library, fqName.asString())
            return protoForNames
        }
}

class KlibMetadataCachedPackageFragment(
    byteArray: ByteArray,
    storageManager: StorageManager,
    module: ModuleDescriptor,
    override val protoForNames: ProtoBuf.PackageFragment = parsePackageFragment(byteArray),
    fqName: FqName = FqName(protoForNames.getExtension(KlibMetadataProtoBuf.fqName))
) :  KlibMetadataPackageFragment(fqName, storageManager, module)

abstract class KlibMetadataPackageFragment(
    fqName: FqName,
    storageManager: StorageManager,
    module: ModuleDescriptor
) : DeserializedPackageFragment(fqName, storageManager, module) {

    lateinit var components: DeserializationComponents

    override fun initialize(components: DeserializationComponents) {
        this.components = components
    }

    // The proto field is lazy so that we can load only needed
    // packages from the library.
    abstract val protoForNames: ProtoBuf.PackageFragment

    open val proto: ProtoBuf.PackageFragment
        get() = protoForNames

    private val nameResolver by lazy {
        NameResolverImpl(protoForNames.strings, protoForNames.qualifiedNames)
    }

    override val classDataFinder by lazy {
        KlibMetadataClassDataFinder(protoForNames, nameResolver)
    }

    private val _memberScope by lazy {
        /* TODO: we fake proto binary versioning for now. */
        DeserializedPackageMemberScope(
            this,
            proto.getPackage(),
            nameResolver,
            KlibMetadataVersion.INSTANCE,
            /* containerSource = */ null,
            components
        ) {
            loadClassNames()
        }
    }

    override fun getMemberScope(): DeserializedPackageMemberScope = _memberScope

    private val classifierNames: Set<Name> by lazy {
        val result = mutableSetOf<Name>()
        result.addAll(loadClassNames())
        protoForNames.getPackage().typeAliasList.mapTo(result) { nameResolver.getName(it.name) }
        result
    }

    fun hasTopLevelClassifier(name: Name): Boolean = name in classifierNames

    private fun loadClassNames(): Collection<Name> {

        val classNameList = protoForNames.getExtension(KlibMetadataProtoBuf.className).orEmpty()

        val names = classNameList.mapNotNull {
            val classId = nameResolver.getClassId(it)
            val shortName = classId.shortClassName
            if (!classId.isNestedClass) shortName else null
        }

        return names
    }
}