/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.toEffectiveVisibilityOrNull
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeLookupTag
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
): EffectiveVisibility =
    toEffectiveVisibilityOrNull(owner, forClass, ownerIsPublishedApi)
        ?: errorWithAttachment("Unknown visibility: $this") {
            withFirLookupTagEntry("owner", owner)
        }