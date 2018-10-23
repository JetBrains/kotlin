/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration

fun ModuleMapping.Companion.loadModuleMapping(
    bytes: ByteArray?,
    debugName: String,
    configuration: DeserializationConfiguration,
    reportIncompatibleVersionError: (JvmMetadataVersion) -> Unit
): ModuleMapping =
    loadModuleMapping(
        bytes,
        debugName,
        configuration.skipMetadataVersionCheck,
        configuration.isJvmPackageNameSupported,
        reportIncompatibleVersionError
    )
