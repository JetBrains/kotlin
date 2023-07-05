package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


class DeprecationsPerUseSite(
    val all: DeprecationInfo?,
    val bySpecificSite: Map<AnnotationUseSiteTarget, DeprecationInfo>?
) {
    fun forUseSite(vararg sites: AnnotationUseSiteTarget): DeprecationInfo? {
        if (bySpecificSite != null) {
            for (site in sites) {
                bySpecificSite[site]?.let { return it }
            }
        }
        return all
    }

    fun isEmpty(): Boolean = all == null && bySpecificSite == null
    fun isNotEmpty(): Boolean = !isEmpty()

    override fun toString(): String =
        if (isEmpty()) "NoDeprecation"
        else "org.jetbrains.kotlin.fir.declarations.DeprecationInfoForUseSites(all=$all, bySpecificSite=$bySpecificSite)"
}

val EmptyDeprecationsPerUseSite = DeprecationsPerUseSite(null, null)
