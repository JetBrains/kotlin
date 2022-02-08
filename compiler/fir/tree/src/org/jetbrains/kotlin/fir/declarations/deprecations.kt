package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.kotlin.utils.keysToMapExceptNulls

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

    fun combineMin(other: DeprecationsPerUseSite): DeprecationsPerUseSite {
        if (isEmpty() || isEmpty()) return EmptyDeprecationsPerUseSite

        return DeprecationsPerUseSite(
            if (all == null || other.all == null) null else minOf(all, other.all),
            if (bySpecificSite == null || other.bySpecificSite == null) {
                null
            } else {
                bySpecificSite.keys.intersect(other.bySpecificSite.keys).keysToMap { target ->
                    minOf(bySpecificSite[target]!!, other.bySpecificSite[target]!!)
                }
            }

        )
    }

    fun combinePreferLeft(other: DeprecationsPerUseSite): DeprecationsPerUseSite {
        return DeprecationsPerUseSite(
            all ?: other.all,
            if (bySpecificSite == null || other.bySpecificSite == null) {
                bySpecificSite ?: other.bySpecificSite
            } else {
                bySpecificSite.keys.union(other.bySpecificSite.keys).keysToMapExceptNulls { target ->
                    bySpecificSite[target] ?: other.bySpecificSite[target]
                }
            }

        )
    }

    fun inheritableOnly(): DeprecationsPerUseSite =
        DeprecationsPerUseSite(
            all?.takeIf { it.propagatesToOverrides },
            bySpecificSite?.filterValues { it.propagatesToOverrides }
        )

    override fun toString(): String =
        if (isEmpty()) "NoDeprecation"
        else "org.jetbrains.kotlin.fir.declarations.DeprecationInfoForUseSites(all=$all, bySpecificSite=$bySpecificSite)"

    companion object {
        fun fromMap(perUseSite: Map<AnnotationUseSiteTarget?, DeprecationInfo>): DeprecationsPerUseSite {
            if (perUseSite.isEmpty()) return EmptyDeprecationsPerUseSite

            @Suppress("UNCHECKED_CAST")
            val specificCallSite = perUseSite.filterKeys { it != null } as Map<AnnotationUseSiteTarget, DeprecationInfo>
            return DeprecationsPerUseSite(
                perUseSite[null],
                specificCallSite.takeIf { it.isNotEmpty() }
            )
        }
    }

}

val EmptyDeprecationsPerUseSite = DeprecationsPerUseSite(null, null)
