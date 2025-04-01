/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.KLIB_PROPERTY_MANUALLY_ENABLED_POISONING_LANGUAGE_FEATURES
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf.Header
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.PreReleaseInfo

class KlibDeserializedContainerSource(
    override val preReleaseInfo: PreReleaseInfo,
    override val presentableString: String,
    val klib: KotlinLibrary,
    override val incompatibility: IncompatibleVersionErrorData<*>?,
) : DeserializedContainerSource {
    constructor(
        klib: KotlinLibrary,
        header: Header,
        configuration: DeserializationConfiguration,
        packageFqName: FqName,
        incompatibility: IncompatibleVersionErrorData<*>?,
    ) : this(
        preReleaseInfo = PreReleaseInfo(
            configuration.reportErrorsOnPreReleaseDependencies && (header.flags and KlibMetadataHeaderFlags.PRE_RELEASE) != 0,
            klib.manifestProperties.propertyList(KLIB_PROPERTY_MANUALLY_ENABLED_POISONING_LANGUAGE_FEATURES).map { it.trimStart('+') }
        ),
        presentableString = "Package '$packageFqName'",
        klib = klib,
        incompatibility = incompatibility,
    )

    override val abiStability: DeserializedContainerAbiStability
        get() = DeserializedContainerAbiStability.STABLE

    // TODO: move [CallableMemberDescriptor.findSourceFile] here.
    override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE
}
