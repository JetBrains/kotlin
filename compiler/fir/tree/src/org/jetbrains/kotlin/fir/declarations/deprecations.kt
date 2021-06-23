package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.kotlin.utils.keysToMapExceptNulls

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


class DeprecationsPerUseSite(
    val all: Deprecation?,
    val bySpecificSite: Map<AnnotationUseSiteTarget, Deprecation>?
) {
    fun forUseSite(vararg sites: AnnotationUseSiteTarget): Deprecation? {
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
            all?.takeIf { it.inheritable },
            bySpecificSite?.filterValues { it.inheritable }
        )

    override fun toString(): String =
        if (isEmpty()) "NoDeprecation"
        else "org.jetbrains.kotlin.fir.declarations.DeprecationInfoForUseSites(all=$all, bySpecificSite=$bySpecificSite)"

    companion object {
        fun fromMap(perUseSite: Map<AnnotationUseSiteTarget?, Deprecation>): DeprecationsPerUseSite {
            if (perUseSite.isEmpty()) return EmptyDeprecationsPerUseSite

            @Suppress("UNCHECKED_CAST")
            val specificCallSite = perUseSite.filterKeys { it != null } as Map<AnnotationUseSiteTarget, Deprecation>
            return DeprecationsPerUseSite(
                perUseSite[null],
                specificCallSite.takeIf { it.isNotEmpty() }
            )
        }
    }

}

data class Deprecation(
    val level: DeprecationLevelValue,
    val inheritable: Boolean,
    val message: String? = null
) : Comparable<Deprecation> {
    override fun compareTo(other: Deprecation): Int {
        val lr = level.compareTo(other.level)
        //to prefer inheritable deprecation
        return if (lr == 0 && !inheritable && other.inheritable) 1
        else lr
    }
}

// values from kotlin.DeprecationLevel
enum class DeprecationLevelValue {
    WARNING, ERROR, HIDDEN
}

val EmptyDeprecationsPerUseSite = DeprecationsPerUseSite(null, null)

