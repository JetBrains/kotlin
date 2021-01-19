/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.impl

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatform
import org.jetbrains.kotlin.test.util.joinToArrayString
import org.jetbrains.kotlin.utils.addToStdlib.runIf

object TargetPlatformParser {
    private const val JVM = "JVM"
    private const val JDK = "JDK"
    private const val JS = "JS"

    fun parseTargetPlatform(declaredPlatforms: List<String>): TargetPlatform? {
        if (declaredPlatforms.isEmpty()) return null
        val simplePlatforms = declaredPlatforms.mapTo(mutableSetOf()) { platformString ->
            tryParseJdkPlatform(platformString)?.let { return@mapTo it }
            tryParseJsPlatform(platformString)?.let { return@mapTo it }
            tryParseNativePlatform(platformString)?.let { return@mapTo it }
            error("Unknown platform: $platformString")
        }
        return TargetPlatform(simplePlatforms)
    }

    private fun tryParseJdkPlatform(platformString: String): JdkPlatform? {
        val target = when {
            platformString == JVM -> JvmTarget.DEFAULT
            !platformString.startsWith(JDK) -> return null
            else -> JvmTarget.values().find { it.name == platformString }
                ?: error("JvmTarget \"$platformString\" not found.\nAvailable targets: ${JvmTarget.values().joinToArrayString()}")
        }
        return JdkPlatform(target)
    }

    private fun tryParseJsPlatform(platformString: String): JsPlatform? {
        return runIf(platformString == JS) { JsPlatforms.DefaultSimpleJsPlatform }
    }

    private fun tryParseNativePlatform(@Suppress("UNUSED_PARAMETER") platformString: String): NativePlatform? {
        // TODO: support native platforms
        return null
    }
}
