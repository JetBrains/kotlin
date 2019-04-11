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
import org.jetbrains.konan.util.CidrKotlinReleaseType.SNAPSHOT
import org.jetbrains.kotlin.konan.library.lite.LiteKonanDistributionInfoProvider
import org.jetbrains.kotlin.utils.addIfNotNull

// Returns Kotlin/Native home.
fun getKotlinNativeHome(project: Project): String? {
    val paths = mutableListOf<String>()
    KonanProjectDataService.forEachKonanProject(project) { konanModel, _, _ ->
        paths.addIfNotNull(konanModel.kotlinNativeHome)
    }

    return paths.firstOrNull()
}

// Returns Kotlin/Native internal version (not the same as Big Kotlin version).
fun getKotlinNativeVersion(kotlinNativeHome: String): CidrKotlinVersion? {
    val fullVersionString = LiteKonanDistributionInfoProvider(kotlinNativeHome)
            .getDistributionInfo()?.kotlinNativeVersionString ?: return null
    return parseFullKotlinVersionString(fullVersionString)
}

// A descriptor of Kotlin/Native for CLion or Kotlin/Native for AppCode plugin.
val cidrKotlinPlugin: IdeaPluginDescriptor by lazy {
    val pluginIn = PluginManager.getPluginByClassName(CidrKotlinVersion::class.java.name)
    PluginManager.getPlugin(pluginIn)!!
}

// The default version of Kotlin (determined by Kotlin/Native plugin version)
val defaultCidrKotlinVersion: CidrKotlinVersion by lazy {
    val pluginVersion = cidrKotlinPlugin.version!!
    val platformPrefix = PlatformUtils.getPlatformPrefix()!!

    val endIndex = pluginVersion.toLowerCase().indexOf("-${platformPrefix.toLowerCase()}").takeIf { it != -1 }
            ?: pluginVersion.length
    val fullKotlinVersion = pluginVersion.substring(0, endIndex)

    parseFullKotlinVersionString(fullKotlinVersion)
            ?: error("""
                |
                |Kotlin/Native plugin version: $pluginVersion
                |Platform prefix: $platformPrefix
                |Evaluated (broken) Kotlin version: $fullKotlinVersion
                """.trimMargin()
            )
}

sealed class CidrKotlinReleaseType {
    interface CidrKotlinReleaseTypeProducer<T : CidrKotlinReleaseType> {
        fun getOrNull(name: String): T?
    }

    object RELEASE : CidrKotlinReleaseType(), CidrKotlinReleaseTypeProducer<RELEASE> {
        override fun getOrNull(name: String): RELEASE? = if (isNameEqual(name)) RELEASE else null
    }

    object DEV : CidrKotlinReleaseType(), CidrKotlinReleaseTypeProducer<DEV> {
        override fun getOrNull(name: String): DEV? = if (isNameEqual(name)) DEV else null
    }

    object EAP : CidrKotlinReleaseType(), CidrKotlinReleaseTypeProducer<EAP> {
        override fun getOrNull(name: String): EAP? = if (isNameEqual(name)) EAP else null
    }

    class RC(val number: Int?) : CidrKotlinReleaseType() {
        companion object : CidrKotlinReleaseTypeProducer<RC> {
            private val prefix = RC::class.java.simpleName!!.toLowerCase()

            override fun getOrNull(name: String): RC? {
                if (!name.startsWith(prefix, ignoreCase = true)) return null

                val remainder = name.substring(prefix.length)
                return if (remainder.isEmpty()) RC(null)
                else RC(remainder.toIntOrNull() ?: return null)
            }
        }
    }

    object SNAPSHOT : CidrKotlinReleaseType(), CidrKotlinReleaseTypeProducer<SNAPSHOT> {
        override fun getOrNull(name: String): SNAPSHOT? = if (isNameEqual(name)) SNAPSHOT else null
    }

    companion object {
        private fun CidrKotlinReleaseType.isNameEqual(name: String) = javaClass.simpleName.equals(name, ignoreCase = true)

        fun findByName(name: String): CidrKotlinReleaseType? =
                RELEASE.getOrNull(name) as CidrKotlinReleaseType?
                        ?: DEV.getOrNull(name)
                        ?: EAP.getOrNull(name)
                        ?: RC.getOrNull(name)
                        ?: SNAPSHOT.getOrNull(name)
    }
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
            .let {
                when (it.size) {
                    2 -> listOf(it[0], it[1], "0")
                    3 -> it
                    else -> null
                }
            }
            ?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.size == 3 }
            ?.let { KotlinVersion(it[0], it[1], it[2]) } ?: return null

    val releaseTypeString = fullKotlinVersionParts[1]
    val releaseType = CidrKotlinReleaseType.findByName(releaseTypeString) ?: return null

    val buildString = when (releaseType) {
        RELEASE, SNAPSHOT -> if (fullKotlinVersionParts.size == 3) return null else null
        else -> if (fullKotlinVersionParts.size == 3) fullKotlinVersionParts[2] else return null
    }

    val build = buildString?.let { it.toIntOrNull() ?: return null }

    return CidrKotlinVersion(fullKotlinVersion, kotlinVersion, releaseType, build)
}
