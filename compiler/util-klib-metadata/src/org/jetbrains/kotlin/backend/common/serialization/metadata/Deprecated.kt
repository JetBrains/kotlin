/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused") // The declarations below may be used outside the Kotlin repo

package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.metadataVersion

@Deprecated(
    "This interface has been moved from package org.jetbrains.kotlin.backend.common.serialization.metadata to package org.jetbrains.kotlin.library.metadata",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.KlibMetadataDeserializedPackageFragmentsFactory")
)
typealias KlibMetadataDeserializedPackageFragmentsFactory = org.jetbrains.kotlin.library.metadata.KlibMetadataDeserializedPackageFragmentsFactory

@Deprecated(
    "This class has been moved from package org.jetbrains.kotlin.backend.common.serialization.metadata to package org.jetbrains.kotlin.library.metadata",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.KlibMetadataVersion")
)
typealias KlibMetadataVersion = org.jetbrains.kotlin.library.metadata.KlibMetadataVersion

@Deprecated(
    "This property has been moved from package org.jetbrains.kotlin.backend.common.serialization.metadata to package org.jetbrains.kotlin.library.metadata",
    ReplaceWith("metadataVersion", "org.jetbrains.kotlin.library.metadata.metadataVersion")
)
inline val KotlinLibrary.metadataVersion: org.jetbrains.kotlin.library.metadata.KlibMetadataVersion?
    get() = metadataVersion

@Deprecated(
    "This interface has been moved from package org.jetbrains.kotlin.backend.common.serialization.metadata to package org.jetbrains.kotlin.library.metadata",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.KlibMetadataModuleDescriptorFactory")
)
typealias KlibMetadataModuleDescriptorFactory = org.jetbrains.kotlin.library.metadata.KlibMetadataModuleDescriptorFactory
