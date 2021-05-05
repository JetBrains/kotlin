/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan

/**
 *  https://en.wikipedia.org/wiki/Software_versioning
 *  scheme major.minor[.build[.revision]].
 */

enum class MetaVersion(val metaString: String) {
    DEV("dev"),
    EAP("eap"),
    BETA("beta"),
    M1("M1"),
    M2("M2"),
    RC("RC"),
    RELEASE("release");

    companion object {
        fun findAppropriate(metaString: String): MetaVersion {
            return values().find { it.metaString.equals(metaString, ignoreCase = true) }
                ?: if (metaString.isBlank()) RELEASE else error("Unknown meta version: $metaString")
        }
    }
}
