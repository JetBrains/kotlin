/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.metadata

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.DescriptorSerializer

// TODO: need a refactoring between IncrementalSerializer and MonolithicSerializer.
class KlibMetadataIncrementalSerializer(
    languageVersionSettings: LanguageVersionSettings,
    metadataVersion: BinaryVersion,
    project: Project,
    exportKDoc: Boolean,
    skipExpects: Boolean,
    allowErrorTypes: Boolean = false
) : KlibMetadataSerializer(languageVersionSettings, metadataVersion, project, exportKDoc, skipExpects, allowErrorTypes = allowErrorTypes) {

    fun serializePackageFragment(
        module: ModuleDescriptor,
        scope: Collection<DeclarationDescriptor>,
        fqName: FqName
    ): ProtoBuf.PackageFragment {

        val allDescriptors = scope.filter {
            it.module == module
        }

        val classifierDescriptors = allDescriptors
            .filterIsInstance<ClassifierDescriptor>()
            .sortedBy { it.fqNameSafe.asString() }

        val topLevelDescriptors = DescriptorSerializer.sort(
            allDescriptors
                .filterIsInstance<CallableDescriptor>()
        )

        // TODO: For now, in the incremental serializer, we assume
        // there is only a single package fragment per file.
        // This is no always the case, actually.
        // But marrying split package fragments with incremental compilation is an endeavour.
        // See monolithic serializer for details.
        return serializeDescriptors(fqName, classifierDescriptors, topLevelDescriptors).single()
    }

    fun serializedMetadata(
        fragments: Map<String, List<ByteArray>>,
        header: ByteArray
    ): SerializedMetadata {
        val fragmentNames = mutableListOf<String>()
        val fragmentParts = mutableListOf<List<ByteArray>>()

        for ((fqName, fragment) in fragments.entries.sortedBy { it.key }) {
            fragmentNames += fqName
            fragmentParts += fragment
        }

        return SerializedMetadata(header, fragmentParts, fragmentNames)
    }

    // TODO: For now, in the incremental serializer, we assume
    // there is only a single package fragment per file.
    // This is no always the case, actually.
    // But marrying split package fragments with incremental compilation is an endeavour.
    // See monolithic serializer for details.
    override val TOP_LEVEL_DECLARATION_COUNT_PER_FILE = null
    override val TOP_LEVEL_CLASS_DECLARATION_COUNT_PER_FILE = null
}