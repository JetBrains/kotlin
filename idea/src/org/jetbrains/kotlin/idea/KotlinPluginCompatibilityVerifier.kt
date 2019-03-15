/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.ui.Messages
import com.intellij.util.PlatformUtils

object KotlinPluginCompatibilityVerifier {
    @JvmStatic
    fun checkCompatibility() {
        val kotlinVersion = KotlinPluginVersion.parse(KotlinPluginUtil.getPluginVersion()) ?: return
        val platformVersion = PlatformVersion.getCurrent() ?: return

        if (kotlinVersion.platformVersion.platform != platformVersion.platform) {
            Messages.showWarningDialog(
                "Installed Kotlin plugin version ($kotlinVersion) is not compatible with $platformVersion.\n" +
                        "Some functionality may be broken.\n\n" +
                        "Please reinstall the Kotlin plugin.",
                "Compatibility issue"
            )
        }
    }
}

internal data class KotlinPluginVersion(
    val kotlinVersion: String, // 1.2.3
    val status: String, // release, eap, rc
    val buildNumber: String, // 53
    val platformVersion: PlatformVersion,
    val patchNumber: String // usually '1'
) {
    companion object {
        private val KOTLIN_VERSION_REGEX = "^([\\d.]+)-([A-Za-z]+)-(\\d+)-([A-Za-z0-9.]+)-(\\d+)$".toRegex()

        fun parse(version: String): KotlinPluginVersion? {
            val matchResult = KOTLIN_VERSION_REGEX.matchEntire(version) ?: return null
            val (kotlinVersion, status, buildNumber, platformString, patchNumber) = matchResult.destructured
            val platformVersion = PlatformVersion.parse(platformString) ?: return null
            return KotlinPluginVersion(kotlinVersion, status, buildNumber, platformVersion, patchNumber)
        }
    }

    override fun toString() = "$kotlinVersion for $platformVersion"
}

internal data class PlatformVersion(val platform: Platform, val version: String /* 3.1 or 2017.3 */) {
    companion object {
        fun parse(platformString: String): PlatformVersion? {
            for (platform in Platform.values()) {
                if (platformString.startsWith(platform.qualifier)) {
                    return PlatformVersion(platform, platformString.drop(platform.qualifier.length))
                }
            }

            return null
        }

        fun getCurrent(): PlatformVersion? {
            val prefix = PlatformUtils.getPlatformPrefix() ?: return null
            val platform = when (prefix) {
                PlatformUtils.IDEA_CE_PREFIX, PlatformUtils.IDEA_PREFIX -> Platform.IDEA
                "AndroidStudio" -> Platform.ANDROID_STUDIO // from 'com.android.tools.idea.IdeInfo'
                else -> return null
            }

            val version = ApplicationInfo.getInstance().run { majorVersion + "." + minorVersion.substringBefore(".") }
            return PlatformVersion(platform, version)
        }
    }

    enum class Platform(val qualifier: String, val presentableText: String) {
        IDEA("IJ", "IDEA"), ANDROID_STUDIO("Studio", "Android Studio")
    }

    override fun toString() = platform.presentableText + " " + version
}