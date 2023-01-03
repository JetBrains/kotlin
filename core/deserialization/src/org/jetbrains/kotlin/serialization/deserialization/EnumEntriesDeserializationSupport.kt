/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization

interface EnumEntriesDeserializationSupport {
    /**
     * Determines whether `Enum.entries` property can be synthesized for enums in this module,
     * when this property is not present in compiled code.
     * Returns `null` if it's not known.
     */
    fun canSynthesizeEnumEntries(): Boolean?

    object Default : EnumEntriesDeserializationSupport {
        override fun canSynthesizeEnumEntries(): Boolean? = null
    }
}

object JvmEnumEntriesDeserializationSupport : EnumEntriesDeserializationSupport {

    // In JVM modules "entries" can be called even on enum compiled without this property.
    override fun canSynthesizeEnumEntries(): Boolean = true
}