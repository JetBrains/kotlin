/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.jvm.deserialization

import org.jetbrains.kotlin.metadata.deserialization.Flags

/**
 * @see Flags
 */
object JvmFlags {
    // Properties

    val IS_MOVED_FROM_INTERFACE_COMPANION = Flags.FlagField.booleanFirst()

    fun getPropertyFlags(isMovedFromInterfaceCompanion: Boolean): Int =
        IS_MOVED_FROM_INTERFACE_COMPANION.toFlags(isMovedFromInterfaceCompanion)
}
