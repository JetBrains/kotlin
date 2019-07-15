/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("KonanUtil")

package org.jetbrains.konan.util

import com.intellij.openapi.project.Project
import com.jetbrains.konan.forEachKonanProject
import org.jetbrains.kotlin.utils.addIfNotNull

// Returns Kotlin/Native home.
fun getKotlinNativeHome(project: Project): String? {
    val paths = mutableListOf<String>()
    forEachKonanProject(project) { konanModel, _, _ ->
        paths.addIfNotNull(konanModel.kotlinNativeHome)
    }

    return paths.firstOrNull()
}
