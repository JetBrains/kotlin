/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.fir.Visibilities
import org.jetbrains.kotlin.fir.Visibility
import org.jetbrains.kotlin.metadata.ProtoBuf

object FirProtoEnumFlags {
    fun visibility(visibility: ProtoBuf.Visibility?): Visibility = when (visibility) {
        ProtoBuf.Visibility.INTERNAL -> Visibilities.INTERNAL
        ProtoBuf.Visibility.PRIVATE -> Visibilities.PRIVATE
        ProtoBuf.Visibility.PRIVATE_TO_THIS -> Visibilities.PRIVATE_TO_THIS
        ProtoBuf.Visibility.PROTECTED -> Visibilities.PROTECTED
        ProtoBuf.Visibility.PUBLIC -> Visibilities.PUBLIC
        ProtoBuf.Visibility.LOCAL -> Visibilities.LOCAL
        else -> Visibilities.PRIVATE
    }

    fun visibility(visibility: Visibility): ProtoBuf.Visibility = when (visibility) {
        Visibilities.INTERNAL -> ProtoBuf.Visibility.INTERNAL
        Visibilities.PUBLIC -> ProtoBuf.Visibility.PUBLIC
        Visibilities.PRIVATE -> ProtoBuf.Visibility.PRIVATE
        Visibilities.PRIVATE_TO_THIS -> ProtoBuf.Visibility.PRIVATE_TO_THIS
        Visibilities.PROTECTED -> ProtoBuf.Visibility.PROTECTED
        Visibilities.LOCAL -> ProtoBuf.Visibility.LOCAL
        else -> throw IllegalArgumentException("Unknown visibility: $visibility")
    }
}
