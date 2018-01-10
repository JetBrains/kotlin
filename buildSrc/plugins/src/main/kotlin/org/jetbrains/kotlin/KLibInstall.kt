package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.Task
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.TargetManager
import java.io.File

// TODO: Implement as a part of the gradle plugin
open class KlibInstall: Exec() {
    @InputFile
    lateinit var klib: File
    var repo: File = project.rootDir

    val installDir: File
        @OutputDirectory
        get() {
            val klibName = klib.name.take(klib.name.lastIndexOf('.'))
            return project.file("${repo.absolutePath}/$klibName")
        }

    @Input
    var target: String = TargetManager.hostName

    override fun configure(config: Closure<*>): Task {
        val result = super.configure(config)
        val konanHome = project.rootProject.file(project.findProperty("konan.home") ?: "dist")
        val suffix = if (TargetManager.host == KonanTarget.MINGW) ".bat" else  ""
        val klibProgram = "$konanHome/bin/klib$suffix"

        doFirst {
            repo.mkdirs()

            commandLine(klibProgram,
                    "install", klib.absolutePath,
                    "-target", target,
                    "-repository", repo.absolutePath
            )
        }
        return result
    }
}