/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.config.KlibAbiCompatibilityLevel
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion

fun KlibAbiCompatibilityLevel.toCInteropKlibMetadataVersion(): MetadataVersion =
    LanguageVersion.fromVersionString("$major.$minor")?.toMetadataVersion() ?: error("Cannot convert $this to MetadataVersion")
