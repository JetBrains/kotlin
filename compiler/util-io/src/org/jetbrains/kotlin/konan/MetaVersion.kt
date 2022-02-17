/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.konan

inline class MetaVersion(val metaString: String) {
    companion object {
        // Following meta versions are left for source-level compatibility
        val DEV = MetaVersion("dev")
        val DEV_GOOGLE = MetaVersion("dev-google-pr")
        val EAP = MetaVersion("eap")
        val BETA = MetaVersion("Beta")
        val M1 = MetaVersion("M1")
        val M2 = MetaVersion("M2")
        val RC = MetaVersion("RC")
        val PUB = MetaVersion("PUB")
        val RELEASE = MetaVersion("release")

        fun findAppropriate(metaString: String): MetaVersion =
            listOf(DEV, DEV_GOOGLE, EAP, BETA, M1, M2, RC, PUB, RELEASE)
                .find { it.metaString.equals(metaString, ignoreCase = true) }
                ?: if (metaString.isBlank()) RELEASE else MetaVersion(metaString)
    }
}
