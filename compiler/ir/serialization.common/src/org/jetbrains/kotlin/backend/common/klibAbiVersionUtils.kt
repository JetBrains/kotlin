/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.customKlibAbiVersion
import org.jetbrains.kotlin.config.klibAbiCompatibilityLevel
import org.jetbrains.kotlin.library.KotlinAbiVersion

fun CompilerConfiguration.klibAbiVersionForManifest(): KotlinAbiVersion {
    return customKlibAbiVersion ?: klibAbiCompatibilityLevel.toAbiVersionForManifest()
}
