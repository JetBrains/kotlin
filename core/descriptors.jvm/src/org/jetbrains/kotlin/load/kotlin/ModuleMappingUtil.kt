/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration

fun ModuleMapping.Companion.loadModuleMapping(
    bytes: ByteArray?,
    debugName: String,
    configuration: DeserializationConfiguration
): ModuleMapping =
    loadModuleMapping(
        bytes,
        debugName,
        { version -> JvmMetadataVersion(*version).isCompatible() },
        configuration.skipMetadataVersionCheck,
        configuration.isJvmPackageNameSupported
    )
