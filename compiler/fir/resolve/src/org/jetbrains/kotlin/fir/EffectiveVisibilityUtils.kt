/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

fun Visibility.toEffectiveVisibility(
    ownerSymbol: FirClassLikeSymbol<*>?,
    checkPublishedApi: Boolean = false
): EffectiveVisibility {
    return toEffectiveVisibility(ownerSymbol?.toLookupTag(), checkPublishedApi)
}

fun Visibility.toEffectiveVisibility(
    owner: ConeClassLikeLookupTag?,
    checkPublishedApi: Boolean = false
): EffectiveVisibility {
    customEffectiveVisibility()?.let { return it }
    return when (this.normalize()) {
        Visibilities.Private, Visibilities.PrivateToThis, Visibilities.InvisibleFake -> EffectiveVisibility.Private
        Visibilities.Protected -> EffectiveVisibility.Protected(owner)
        Visibilities.Internal -> when (!checkPublishedApi /*|| !owner.isPublishedApi()*/) { // TODO
            true -> EffectiveVisibility.Internal
            false -> EffectiveVisibility.Public
        }
        Visibilities.Public -> EffectiveVisibility.Public
        Visibilities.Local -> EffectiveVisibility.Local
        else -> error("Unknown visibility: $this")
    }
}
