/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("KonanUtil")

package org.jetbrains.konan.util

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.project.Project
import org.jetbrains.konan.gradle.KonanProjectDataService
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.library.lite.LiteKonanDistributionProvider
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File

// Returns Kotlin/Native home.
fun getKotlinNativeHome(project: Project): String? {
    val paths = mutableListOf<String>()
    KonanProjectDataService.forEachKonanProject(project) { konanModel, _, _ ->
        paths.addIfNotNull(konanModel.kotlinNativeHome)
    }

    return paths.firstOrNull()
}

// Returns Kotlin/Native internal version (not the same as Big Kotlin version).
fun getKotlinNativeVersion(kotlinNativeHome: String): KonanVersion? {
    return LiteKonanDistributionProvider.getDistribution(File(kotlinNativeHome))?.konanVersion
}

// A descriptor of Kotlin/Native for CLion or Kotlin/Native for AppCode plugin.
val cidrKotlinPlugin: IdeaPluginDescriptor by lazy {
    val pluginIn = PluginManager.getPluginByClassName(ForClassloader::class.java.name)
    PluginManager.getPlugin(pluginIn)!!
}

private object ForClassloader
