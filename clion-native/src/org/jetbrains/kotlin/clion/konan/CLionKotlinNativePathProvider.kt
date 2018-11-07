/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.clion.konan

import com.intellij.openapi.project.Project
import org.jetbrains.konan.gradle.KonanProjectDataService
import org.jetbrains.kotlin.cidr.konan.KotlinNativePathProvider
import org.jetbrains.kotlin.utils.addIfNotNull

class CLionKotlinNativePathProvider(val project: Project) : KotlinNativePathProvider {

    override fun getKotlinNativePath(): String? {
        val paths = mutableListOf<String>()
        KonanProjectDataService.forEachKonanProject(project) { konanModel, _, _ ->
            paths.addIfNotNull(konanModel.kotlinNativeHome)
        }

        return paths.firstOrNull()
    }
}
