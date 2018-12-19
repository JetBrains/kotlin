/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("KonanUtil")

package org.jetbrains.konan.util

import com.intellij.openapi.project.Project
import org.jetbrains.konan.gradle.KonanProjectDataService
import org.jetbrains.kotlin.utils.addIfNotNull

fun getKotlinNativePath(project: Project): String? {
    val paths = mutableListOf<String>()
    KonanProjectDataService.forEachKonanProject(project) { konanModel, _, _ ->
        paths.addIfNotNull(konanModel.kotlinNativeHome)
    }

    return paths.firstOrNull()
}
