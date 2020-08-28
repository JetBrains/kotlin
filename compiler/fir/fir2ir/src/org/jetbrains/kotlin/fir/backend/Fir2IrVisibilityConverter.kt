/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.Visibilities
import org.jetbrains.kotlin.fir.Visibility
import org.jetbrains.kotlin.descriptors.Visibility as OldVisibility
import org.jetbrains.kotlin.descriptors.Visibilities as OldVisibilities

abstract class Fir2IrVisibilityConverter {
    object Default : Fir2IrVisibilityConverter() {
        override fun convertPlatformVisibility(visibility: Visibility): OldVisibility {
            error("Unknown visibility: $this")
        }
    }

    fun convertToOldVisibility(visibility: Visibility): OldVisibility {
        return when (visibility) {
            Visibilities.Private -> OldVisibilities.PRIVATE
            Visibilities.PrivateToThis -> OldVisibilities.PRIVATE_TO_THIS
            Visibilities.Protected -> OldVisibilities.PROTECTED
            Visibilities.Internal -> OldVisibilities.INTERNAL
            Visibilities.Public -> OldVisibilities.PUBLIC
            Visibilities.Local -> OldVisibilities.LOCAL
            Visibilities.InvisibleFake -> OldVisibilities.INVISIBLE_FAKE
            Visibilities.Unknown -> OldVisibilities.UNKNOWN
            else -> convertPlatformVisibility(visibility)
        }
    }

    protected abstract fun convertPlatformVisibility(visibility: Visibility): OldVisibility
}
