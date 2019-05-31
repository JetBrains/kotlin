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
    ALPHA("alpha"),
    BETA("beta"),
    RC1("rc1"),
    RC2("rc2"),
    RELEASE("release");

    companion object {

        fun findAppropriate(metaString: String): MetaVersion {
            return MetaVersion.values().find { it.metaString.equals(metaString, ignoreCase = true) }
                ?: if (metaString.isBlank()) RELEASE else error("Unknown meta version: $metaString")
        }
    }
}
