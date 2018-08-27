/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.SystemProperties.getUserHome
import com.intellij.util.io.exists
import com.intellij.util.net.IOExceptionDialog
import org.jetbrains.konan.settings.KonanProjectComponent
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.util.DependencyDownloaderHTTPResponseException
import org.jetbrains.kotlin.konan.util.DependencyProcessor
import org.jetbrains.kotlin.konan.util.DependencySource
import java.nio.file.Path
import java.nio.file.Paths

class KotlinNativeToolchain(
    val version: String,
    val repoUrl: String
) {
    private val artifactName = "kotlin-native-$KONAN_OS-$version"
    val baseDir: Path get() = Paths.get("${getUserHome()}/.konan/$artifactName")
    val konanc: Path get() = baseDir.resolve("bin/konanc")
    val cinterop: Path get() = baseDir.resolve("bin/cinterop")


    fun ensureExists(project: Project) {
        ApplicationManager.getApplication().assertIsDispatchThread()
        if (baseDir.exists()) return

        for (attempt in 0..3) {
            var exception: Throwable? = null
            object : Task.Modal(project, "Downloading Kotlin/Native $version", false) {
                override fun run(progress: ProgressIndicator) {
                    DependencyProcessor(
                        baseDir.parent.toFile(),
                        repoUrl,
                        mapOf(artifactName to listOf(DependencySource.Remote.Public)),
                        customProgressCallback = { _, downloaded, total ->
                            progress.fraction = downloaded / maxOf(total, downloaded, 1).toDouble()
                        }
                    ).run()
                    ApplicationManager.getApplication().invokeLater {
                        project.getComponent(KonanProjectComponent::class.java).reloadLibraries()
                    }
                }

                override fun onThrowable(error: Throwable) {
                    exception = error
                }
            }.queue()

            val ex = exception
            val tryAgain = if (ex == null) {
                false
            } else {
                val details = if (ex is DependencyDownloaderHTTPResponseException) {
                    "Server returned ${ex.responseCode} when trying to download ${ex.url}."
                } else {
                    LOG.error(ex)
                    "Unknown error occurred."
                }
                IOExceptionDialog.showErrorDialog(
                    "Failed to download Kotlin/Native",
                    details
                )
            }
            if (!tryAgain) break
        }
    }

    companion object {
        fun looksLikeBundledToolchain(path: String): Boolean =
            Paths.get(path).startsWith(Paths.get("${getUserHome()}/.konan/"))

        private val KONAN_OS = HostManager.simpleOsName()

        //todo: fixme
        private val BUNDLED_VERSION = "0.8" //bundledFile("kotlin-native-version").readText().trim()
        private val BUILD_DIR = "releases"

        val BUNDLED = KotlinNativeToolchain(
            version = BUNDLED_VERSION,
            repoUrl = "https://download.jetbrains.com/kotlin/native/builds/$BUILD_DIR/$BUNDLED_VERSION/$KONAN_OS"
        )

        private val LOG = Logger.getInstance(KotlinNativeToolchain::class.java)
    }
}
