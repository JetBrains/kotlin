/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration

fun DeserializationConfiguration.metadataVersionOrDefault(): MetadataVersion =
    binaryVersion as? MetadataVersion ?: MetadataVersion.INSTANCE