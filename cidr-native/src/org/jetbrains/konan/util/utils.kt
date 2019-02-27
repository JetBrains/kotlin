/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("KonanUtil")

package org.jetbrains.konan.util

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.project.Project
import com.intellij.util.PlatformUtils
import org.jetbrains.konan.gradle.KonanProjectDataService
import org.jetbrains.konan.util.CidrKotlinReleaseType.RELEASE
import org.jetbrains.kotlin.konan.library.lite.LiteKonanDistributionInfoProvider
import org.jetbrains.kotlin.utils.addIfNotNull

// Returns Kotlin/Native path.
fun getKotlinNativePath(project: Project): String? {
    val paths = mutableListOf<String>()
    KonanProjectDataService.forEachKonanProject(project) { konanModel, _, _ ->
        paths.addIfNotNull(konanModel.kotlinNativeHome)
    }

    return paths.firstOrNull()
}

// Returns Kotlin/Native internal version (not the same as Big Kotlin version).
fun getKotlinNativeVersion(kotlinNativeHome: String): KotlinVersion? =
    LiteKonanDistributionInfoProvider(kotlinNativeHome).getDistributionInfo()?.kotlinNativeVersion

// A descriptor of Kotlin/Native for CLion or Kotlin/Native for AppCode plugin.
val cidrKotlinPlugin: IdeaPluginDescriptor by lazy {
    val pluginIn = PluginManager.getPluginByClassName(CidrKotlinVersion::class.java.name)
    PluginManager.getPlugin(pluginIn)!!
}

// The default version of Kotlin (determined by Kotlin/Native plugin version)
val defaultCidrKotlinVersion: CidrKotlinVersion by lazy {
    val pluginVersion = cidrKotlinPlugin.version!!.toLowerCase()
    val platformPrefix = PlatformUtils.getPlatformPrefix()!!.toLowerCase()

    val fullKotlinVersion = pluginVersion.substringBefore("-$platformPrefix")
    parseFullKotlinVersionString(fullKotlinVersion) ?: error("Invalid Kotlin/Native plugin version: $pluginVersion")
}

enum class CidrKotlinReleaseType {
    RELEASE, DEV, EAP
}

data class CidrKotlinVersion(
        val fullVersionString: String,
        val kotlinVersion: KotlinVersion,
        val releaseType: CidrKotlinReleaseType,
        val build: Int?
) {
    override fun toString() = fullVersionString
}

private fun parseFullKotlinVersionString(fullKotlinVersion: String): CidrKotlinVersion? {
    val fullKotlinVersionParts = fullKotlinVersion.split('-')
    if (fullKotlinVersionParts.size !in 2..3) return null

    val kotlinVersionString = fullKotlinVersionParts[0]
    val kotlinVersion = kotlinVersionString.split('.')
            .takeIf { it.size == 3 }
            ?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.size == 3 }
            ?.let { KotlinVersion(it[0], it[1], it[2]) } ?: return null

    val releaseTypeString = fullKotlinVersionParts[1]
    val releaseType = CidrKotlinReleaseType.values()
            .firstOrNull { it.name.equals(releaseTypeString, ignoreCase = true) } ?: return null

    val buildString = when (releaseType) {
        RELEASE -> if (fullKotlinVersionParts.size == 3) return null else null
        else -> if (fullKotlinVersionParts.size == 3) fullKotlinVersionParts[2] else return null
    }

    val build = buildString?.let { it.toIntOrNull() ?: return null }

    return CidrKotlinVersion(fullKotlinVersion, kotlinVersion, releaseType, build)
}
