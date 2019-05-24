/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import java.util.*

object KotlinPluginUtil {

    val KOTLIN_PLUGIN_ID: PluginId = PluginManagerCore.getPluginByClassName(KotlinPluginUtil::class.java.name)
        ?: PluginId.getId("org.jetbrains.kotlin")

    private const val PATCHED_PLUGIN_VERSION_KEY = "kotlin.plugin.version"
    private val PATCHED_PLUGIN_VERSION: String? = System.getProperty(PATCHED_PLUGIN_VERSION_KEY, null)

    @JvmStatic
    fun getPluginVersion(): String {
        if (PATCHED_PLUGIN_VERSION != null) {
            assert(isPatched())
            return PATCHED_PLUGIN_VERSION
        }

        @Suppress("DEPRECATION")
        return getPluginVersionFromIdea()
    }

    @Deprecated("This method returns original plugin version. Please use getPluginVersion() instead.")
    @JvmStatic
    fun getPluginVersionFromIdea(): String {
        val plugin = PluginManager.getPlugin(KOTLIN_PLUGIN_ID)
            ?: error("Kotlin plugin not found: " + Arrays.toString(PluginManagerCore.getPlugins()))
        return plugin.version
    }

    @JvmStatic
    fun isPatched(): Boolean {
        return PATCHED_PLUGIN_VERSION != null
    }

    @JvmStatic
    fun isSnapshotVersion(): Boolean {
        return "@snapshot@" == getPluginVersion()
    }

    fun isDevVersion(): Boolean {
        return getPluginVersion().contains("-dev-")
    }
}
