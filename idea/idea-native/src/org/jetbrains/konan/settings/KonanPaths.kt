package org.jetbrains.konan.settings

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.util.io.exists
import com.intellij.util.io.isDirectory
import org.jetbrains.konan.KotlinNativeToolchain
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

open class KonanPaths(protected val project: Project) : ProjectComponent {
    companion object {
        private const val STDLIB_PATH = "klib/common/stdlib" //todo: move to kotlin/native

        fun bundledKonanDist(): Path = KotlinNativeToolchain.BUNDLED.baseDir

        fun getInstance(project: Project): KonanPaths = project.getComponent(KonanPaths::class.java)
    }

    override fun getComponentName() = "Konan Compiler Paths"

    fun konanStdlib(): Path? = konanDist()?.resolve(STDLIB_PATH)

    open fun konanDist(): Path? {
        for (provider in KonanModelProvider.EP_NAME.extensions) {
            return provider.getKonanHome(project)
        }
        return bundledKonanDist()
    }

    open fun libraryPaths(): Set<Path> = emptySet()

    fun konanPlatformLibraries(): List<Path> {
        val resolvedTargetName = HostManager.resolveAlias(target().name)
        val klibPath = konanDist()?.resolve("klib/platform/${resolvedTargetName}") ?: return emptyList()
        if (!klibPath.exists()) return emptyList()
        return Files.walk(klibPath, 1).filter {
            it.isDirectory() && it.fileName.toString() != "stdlib" && it != klibPath
        }.collect(Collectors.toList())
    }

    //todo: this is wrong, we are not allowing multiple targets in project
    open fun target(): KonanTarget = HostManager.host
}