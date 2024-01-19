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
import org.jetbrains.kotlin.fir.utils.exceptions.withFirLookupTagEntry
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

fun Visibility.toEffectiveVisibility(
    ownerSymbol: FirClassLikeSymbol<*>?,
    forClass: Boolean = false,
    checkPublishedApi: Boolean = false
): EffectiveVisibility {
    return toEffectiveVisibility(ownerSymbol?.toLookupTag(), forClass, checkPublishedApi)
}

fun Visibility.toEffectiveVisibility(
    owner: ConeClassLikeLookupTag?,
    forClass: Boolean = false,
    ownerIsPublishedApi: Boolean = false
): EffectiveVisibility {
    customEffectiveVisibility()?.let { return it }
    return when (this.normalize()) {
        Visibilities.PrivateToThis, Visibilities.InvisibleFake -> EffectiveVisibility.PrivateInClass
        Visibilities.Private -> if (owner == null && forClass) EffectiveVisibility.PrivateInFile else EffectiveVisibility.PrivateInClass
        Visibilities.Protected -> EffectiveVisibility.Protected(owner)
        Visibilities.Internal -> when (ownerIsPublishedApi) {
            true -> EffectiveVisibility.Public
            false -> EffectiveVisibility.Internal
        }
        Visibilities.Public -> EffectiveVisibility.Public
        Visibilities.Local -> EffectiveVisibility.Local
        // Unknown visibility should not provoke errors,
        // because they can naturally appear when intersection
        // overrides' bases have inconsistent forms.
        Visibilities.Unknown -> EffectiveVisibility.Unknown
        else -> errorWithAttachment("Unknown visibility: $this") {
            withFirLookupTagEntry("owner", owner)
        }
    }
}
