/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.Visibility

interface FirEffectiveVisibility {

    val name: String

    val publicApi: Boolean

    val privateApi: Boolean

    enum class Permissiveness {
        LESS,
        SAME,
        MORE,
        UNKNOWN
    }

    fun relation(other: FirEffectiveVisibility): Permissiveness

    fun toVisibility(): Visibility

    fun lowerBound(other: FirEffectiveVisibility): FirEffectiveVisibility

    object Default : FirEffectiveVisibility {
        override val name: String
            get() = "???"
        override val publicApi: Boolean
            get() = false
        override val privateApi: Boolean
            get() = false

        override fun relation(other: FirEffectiveVisibility): Permissiveness {
            throw AssertionError("Should not be called")
        }

        override fun toVisibility(): Visibility {
            throw AssertionError("Should not be called")
        }

        override fun lowerBound(other: FirEffectiveVisibility): FirEffectiveVisibility {
            throw AssertionError("Should not be called")
        }
    }
}

