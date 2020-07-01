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
