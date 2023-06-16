/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.irProviderName
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf.Header
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class KlibDeserializedContainerSource(
    override val isPreReleaseInvisible: Boolean,
    override val presentableString: String,
    val isFromNativeInteropLibrary: Boolean
) : DeserializedContainerSource {

    constructor(
        library: KotlinLibrary,
        header: Header,
        configuration: DeserializationConfiguration,
        packageFqName: FqName
    ) : this(
        configuration.reportErrorsOnPreReleaseDependencies &&
                (header.flags and KlibMetadataHeaderFlags.PRE_RELEASE) != 0,
        "Package '$packageFqName'",
        library.isInteropLibrary()
    )

    override val incompatibility: IncompatibleVersionErrorData<*>?
        get() = null // TODO KT-55808

    override val abiStability: DeserializedContainerAbiStability
        get() = DeserializedContainerAbiStability.STABLE

    // TODO: move [CallableMemberDescriptor.findSourceFile] here.
    override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE
}

private fun KotlinLibrary.isInteropLibrary() = irProviderName == KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
