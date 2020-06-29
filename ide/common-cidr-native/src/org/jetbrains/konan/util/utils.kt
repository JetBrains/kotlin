/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("KonanUtil")

package org.jetbrains.konan.util

import com.intellij.openapi.project.Project
import org.jetbrains.konan.gradle.forEachKonanProject
import org.jetbrains.kotlin.utils.addIfNotNull

// Returns Kotlin/Native home.
fun getKotlinNativeHome(project: Project): String? {
    forEachKonanProject(project) { konanModel, _, _ ->
        konanModel.kotlinNativeHome?.let { return it }
    }
    return null
}
