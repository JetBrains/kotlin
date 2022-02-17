/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.konan

inline class MetaVersion(val metaString: String) {
    operator fun compareTo(other: MetaVersion): Int {
        if (metaOrder.contains(this)) {
            if (metaOrder.contains(other)) {
                return metaOrder.indexOf(this).compareTo(metaOrder.indexOf(other))
            }
        }
        return metaString.compareTo(other.metaString)
    }

    companion object {
        // Following meta versions are left for source-level compatibility
        val DEV = MetaVersion("dev")
        val DEV_GOOGLE = MetaVersion("dev-google-pr")
        val EAP = MetaVersion("eap")
        val BETA = MetaVersion("Beta")
        val RC = MetaVersion("RC")
        val PUB = MetaVersion("PUB")
        val RELEASE = MetaVersion("release")

        // Defines order of meta versions
        private val metaOrder = listOf(DEV, DEV_GOOGLE, EAP, BETA, RC, PUB, RELEASE)

        fun findAppropriate(metaString: String): MetaVersion = metaOrder
            .find { it.metaString.equals(metaString, ignoreCase = true) }
            ?: if (metaString.isBlank()) RELEASE else MetaVersion(metaString)  // should it be lowercased?
    }
}
