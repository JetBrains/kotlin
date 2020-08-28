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
        ProtoBuf.Visibility.INTERNAL -> Visibilities.Internal
        ProtoBuf.Visibility.PRIVATE -> Visibilities.Private
        ProtoBuf.Visibility.PRIVATE_TO_THIS -> Visibilities.PrivateToThis
        ProtoBuf.Visibility.PROTECTED -> Visibilities.Protected
        ProtoBuf.Visibility.PUBLIC -> Visibilities.Public
        ProtoBuf.Visibility.LOCAL -> Visibilities.Local
        else -> Visibilities.Private
    }

    fun visibility(visibility: Visibility): ProtoBuf.Visibility = when (visibility) {
        Visibilities.Internal -> ProtoBuf.Visibility.INTERNAL
        Visibilities.Public -> ProtoBuf.Visibility.PUBLIC
        Visibilities.Private -> ProtoBuf.Visibility.PRIVATE
        Visibilities.PrivateToThis -> ProtoBuf.Visibility.PRIVATE_TO_THIS
        Visibilities.Protected -> ProtoBuf.Visibility.PROTECTED
        Visibilities.Local -> ProtoBuf.Visibility.LOCAL
        else -> throw IllegalArgumentException("Unknown visibility: $visibility")
    }
}
