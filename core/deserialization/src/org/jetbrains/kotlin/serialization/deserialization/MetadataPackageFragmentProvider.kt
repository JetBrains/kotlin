/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.deserialization.AdditionalClassPartsProvider
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentDeclarationFilter
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.ChainedMemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.ClassData
import org.jetbrains.kotlin.serialization.ClassDataWithSource
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.storage.StorageManager
import java.io.InputStream

class MetadataPackageFragmentProvider(
        storageManager: StorageManager,
        finder: KotlinMetadataFinder,
        moduleDescriptor: ModuleDescriptor,
        notFoundClasses: NotFoundClasses,
        private val packagePartProvider: PackagePartProvider
) : AbstractDeserializedPackageFragmentProvider(storageManager, finder, moduleDescriptor) {
    init {
        components = DeserializationComponents(
                storageManager,
                moduleDescriptor,
                DeserializationConfiguration.Default, // TODO
                DeserializedClassDataFinder(this),
                AnnotationAndConstantLoaderImpl(moduleDescriptor, notFoundClasses, BuiltInSerializerProtocol),
                this,
                LocalClassifierTypeSettings.Default,
                ErrorReporter.DO_NOTHING,
                LookupTracker.DO_NOTHING,
                FlexibleTypeDeserializer.ThrowException,
                emptyList(),
                notFoundClasses, AdditionalClassPartsProvider.None, PlatformDependentDeclarationFilter.All
        )
    }

    override fun findPackage(fqName: FqName): DeserializedPackageFragment? =
            if (finder.hasMetadataPackage(fqName))
                MetadataPackageFragment(fqName, storageManager, moduleDescriptor, packagePartProvider, finder)
            else null
}

class MetadataPackageFragment(
        fqName: FqName,
        storageManager: StorageManager,
        module: ModuleDescriptor,
        private val packagePartProvider: PackagePartProvider,
        private val finder: KotlinMetadataFinder
) : DeserializedPackageFragment(fqName, storageManager, module) {
    override val classDataFinder = ClassDataFinder { classId ->
        val topLevelClassId = generateSequence(classId, ClassId::getOuterClassId).last()
        val stream = finder.findMetadata(topLevelClassId) ?: return@ClassDataFinder null
        val (message, nameResolver) = readProto(stream)
        message.class_List.firstOrNull { classProto ->
            nameResolver.getClassId(classProto.fqName) == classId
        }?.let { classProto ->
            ClassDataWithSource(ClassData(nameResolver, classProto), SourceElement.NO_SOURCE)
        }
    }

    override fun computeMemberScope(): MemberScope {
        // For each .kotlin_metadata file which represents a package part, add a separate deserialized scope
        // with top level callables and type aliases (but no classes) only from that part
        val packageParts = packagePartProvider.findMetadataPackageParts(fqName.asString())
        val scopes = arrayListOf<DeserializedPackageMemberScope>()
        for (partName in packageParts) {
            val stream = finder.findMetadata(ClassId(fqName, Name.identifier(partName))) ?: continue
            val (proto, nameResolver) = readProto(stream)

            scopes.add(DeserializedPackageMemberScope(
                    this, proto.`package`, nameResolver, containerSource = null, components = components, classNames = { emptyList() }
            ))
        }

        // Also add the deserialized scope that can load all classes from this package
        scopes.add(object : DeserializedPackageMemberScope(
                this, ProtoBuf.Package.getDefaultInstance(),
                NameResolverImpl(ProtoBuf.StringTable.getDefaultInstance(), ProtoBuf.QualifiedNameTable.getDefaultInstance()),
                containerSource = null, components = components, classNames = { emptyList() }
        ) {
            override fun hasClass(name: Name): Boolean = hasTopLevelClass(name)
            override fun definitelyDoesNotContainName(name: Name, location: LookupLocation) = false
            override fun getClassifierNames(): Set<Name>? = null
        })

        return ChainedMemberScope.create("Metadata scope", scopes)
    }

    override fun hasTopLevelClass(name: Name): Boolean {
        // TODO: check if the corresponding file exists
        return true
    }

    private fun readProto(stream: InputStream): Pair<ProtoBuf.PackageFragment, NameResolverImpl> {
        val version = BuiltInsBinaryVersion.readFrom(stream)

        if (!version.isCompatible()) {
            // TODO: report a proper diagnostic
            throw UnsupportedOperationException(
                    "Kotlin metadata definition format version is not supported: " +
                    "expected ${BuiltInsBinaryVersion.INSTANCE}, actual $version. " +
                    "Please update Kotlin"
            )
        }

        val message = ProtoBuf.PackageFragment.parseFrom(stream, BuiltInSerializerProtocol.extensionRegistry)
        val nameResolver = NameResolverImpl(message.strings, message.qualifiedNames)
        return Pair(message, nameResolver)
    }

    companion object {
        val DOT_METADATA_FILE_EXTENSION = ".kotlin_metadata"
        val METADATA_FILE_EXTENSION = "kotlin_metadata"
    }
}
