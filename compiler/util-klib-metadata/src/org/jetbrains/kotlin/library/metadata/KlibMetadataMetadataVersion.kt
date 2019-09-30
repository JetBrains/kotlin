/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

class KlibMetadataVersion(vararg numbers: Int) : BinaryVersion(*numbers) {

    override fun isCompatible(): Boolean = this.major == 1 && this.minor == 0

    companion object {
        @JvmField
        val INSTANCE = KlibMetadataVersion(1, 0, 0)

        @JvmField
        val INVALID_VERSION = KlibMetadataVersion()
    }
}
