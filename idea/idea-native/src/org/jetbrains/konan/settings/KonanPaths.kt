/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.settings

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.util.io.exists
import com.intellij.util.io.isDirectory
import org.jetbrains.konan.KotlinNativeToolchain
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

open class KonanPaths(protected val project: Project) : ProjectComponent {
    companion object {
        fun getInstance(project: Project): KonanPaths = project.getComponent(KonanPaths::class.java)

        fun bundledKonanDist(): Path = KotlinNativeToolchain.BUNDLED.baseDir
    }

    override fun getComponentName() = "Kotlin/Native Compiler Paths"

    fun konanStdlib(): Path? = konanDist()?.resolve(konanCommonLibraryPath(KONAN_STDLIB_NAME))

    open fun konanDist(): Path? {
        for (provider in KonanModelProvider.EP_NAME.extensions) {
            return provider.getKonanHome(project)
        }
        return bundledKonanDist()
    }

    open fun libraryPaths(): Set<Path> = emptySet()

    fun konanPlatformLibraries(): List<Path> {

        val resolvedTargetName = HostManager.resolveAlias(target().name)
        val klibPath =
            konanDist()?.resolve(konanSpecificPlatformLibrariesPath(resolvedTargetName))?.takeIf { it.exists() } ?: return emptyList()

        return Files.walk(klibPath, 1).filter {
            it.isDirectory() && it.fileName.toString() != KONAN_STDLIB_NAME && it != klibPath
        }.collect(Collectors.toList())
    }

    //todo: this is wrong, we are not allowing multiple targets in project
    open fun target(): KonanTarget = HostManager.host
}
