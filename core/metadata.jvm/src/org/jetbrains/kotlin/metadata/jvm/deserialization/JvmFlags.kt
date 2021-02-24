/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.jvm.deserialization

import org.jetbrains.kotlin.metadata.deserialization.Flags

/**
 * @see Flags
 */
object JvmFlags {
    // Properties
    val IS_MOVED_FROM_INTERFACE_COMPANION = Flags.FlagField.booleanFirst()

    //Class
    val ARE_INTERFACE_METHOD_BODIES_INSIDE = Flags.FlagField.booleanFirst()
    val IS_ALL_COMPATIBILITY_MODE = Flags.FlagField.booleanAfter(ARE_INTERFACE_METHOD_BODIES_INSIDE)

    fun getPropertyFlags(isMovedFromInterfaceCompanion: Boolean): Int =
        IS_MOVED_FROM_INTERFACE_COMPANION.toFlags(isMovedFromInterfaceCompanion)

    fun getClassFlags(isAllInterfaceBodiesInside: Boolean, isAllCompatibilityMode: Boolean): Int =
        ARE_INTERFACE_METHOD_BODIES_INSIDE.toFlags(isAllInterfaceBodiesInside) or IS_ALL_COMPATIBILITY_MODE.toFlags(isAllCompatibilityMode)

}
