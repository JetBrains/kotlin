/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.loader

import java.util.Properties

/**
 * An extension of [KlibLoader] that allows doing some pre-processing of the manifest properties.
 */
interface KlibManifestTransformer {
    fun transform(manifestProperties: Properties): Properties
}
