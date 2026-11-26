/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.daemonExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.nativeImageDaemonExecutionPolicy
import org.junit.jupiter.api.io.TempDir
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries

@TestDataPath("\$CONTENT_ROOT/../main/resources/modules")
abstract class BaseCompilationTest {
    @TempDir
    lateinit var workingDirectory: Path

    fun runSingleShotDaemonTest(
        kotlinToolchains: KotlinToolchains,
        additionalDaemonConfiguration: ExecutionPolicy.WithDaemon.Builder.() -> Unit = {},
        additionalCleanupActions: (daemonRunPath: Path) -> Unit = {},
        body: (ExecutionPolicy.WithDaemon, daemonRunPath: Path) -> Unit,
    ) = runSingleShot(
        "test-daemon-files",
        additionalCleanupActions,
        createPolicy = { daemonRunPath ->
            kotlinToolchains.daemonExecutionPolicy {
                this[ExecutionPolicy.WithDaemon.DAEMON_RUN_DIR_PATH] = daemonRunPath
                this[ExecutionPolicy.WithDaemon.SHUTDOWN_DELAY_MILLIS] = 0
                additionalDaemonConfiguration()
            }
        },
    ) { daemonPolicy, daemonRunPath ->
        body(daemonPolicy, daemonRunPath)
    }

    fun runSingleShotNativeImageDaemonTest(
        kotlinToolchains: KotlinToolchains,
        distPath: Path,
        additionalDaemonConfiguration: ExecutionPolicy.NativeImageDaemon.Builder.() -> Unit = {},
        additionalCleanupActions: (daemonRunPath: Path) -> Unit = {},
        body: (ExecutionPolicy.NativeImageDaemon, daemonRunPath: Path) -> Unit,
    ) = runSingleShot(
        "test-native-image-daemon-files",
        additionalCleanupActions,
        createPolicy = { daemonRunPath ->
            kotlinToolchains.nativeImageDaemonExecutionPolicy(distPath) {
                this[ExecutionPolicy.NativeImageDaemon.DAEMON_RUN_DIR_PATH] = daemonRunPath
                this[ExecutionPolicy.NativeImageDaemon.SHUTDOWN_DELAY_MILLIS] = 0
                additionalDaemonConfiguration()
            }
        },
    ) { daemonPolicy, daemonRunPath ->
        body(daemonPolicy, daemonRunPath)
    }

    private fun <P> runSingleShot(
        daemonFilesPrefix: String,
        additionalCleanupActions: (daemonRunPath: Path) -> Unit,
        createPolicy: (daemonRunPath: Path) -> P,
        body: (policy: P, daemonRunPath: Path) -> Unit,
    ) {
        val daemonRunPath: Path = createTempDirectory(daemonFilesPrefix)
        try {
            body(createPolicy(daemonRunPath), daemonRunPath)
        } finally {
            attemptCleanupDaemon(daemonRunPath, additionalCleanupActions)
        }
    }

    /**
     * It's essential that we wait for the daemon to shut down before attempting to delete the test directory, otherwise (on Windows)
     * an Exception will be thrown saying that the directory is in use and cannot be deleted.
     *
     * One way for telling a daemon to shut down is to delete its ".run" file, then wait for it to notice that the file is gone, in which
     * case the daemon will eventually finish its process.
     */
    private fun attemptCleanupDaemon(daemonRunPath: Path, additionalCleanupActions: (daemonRunPath: Path) -> Unit) {
        additionalCleanupActions(daemonRunPath)
        var tries = 10
        do {
            val deleted = try {
                daemonRunPath.listDirectoryEntries("*.run").forEach { it.deleteIfExists() }
                daemonRunPath.deleteExisting()
                true // run file AND daemon directory deletion was successful, which means daemon is gone now
            } catch (_: NoSuchFileException) {
                true // the daemon directory was already deleted, which means daemon is gone now
            } catch (_: Exception) {
                false // we weren't able to delete the daemon directory, so the daemon might still be running
            }
            if (deleted) {
                break
            }
            Thread.sleep(150)
        } while (tries-- > 0)
    }
}
