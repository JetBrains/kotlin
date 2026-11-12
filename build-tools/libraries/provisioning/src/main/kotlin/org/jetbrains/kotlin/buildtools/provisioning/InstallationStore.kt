/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption

private const val DEFAULT_MARKER_FILE_NAME = ".provisioned"
private const val LOCKS_DIRECTORY = ".locks"
private const val STAGING_DIRECTORY = ".staging"
private const val PUBLISH_ATTEMPTS = 5
private const val PUBLISH_RETRY_DELAY_MILLIS = 100L

/**
 * A directory with installations of one kind, shared by every build on the machine, for example `~/.konan`.
 *
 * Each installation is a subdirectory named by its id. A directory with that name only ever appears complete, with the
 * marker file inside, and is never changed afterwards, so it is safe to read while other installations are being added.
 * Installing is safe when several threads, class loaders or processes ask for the same id at once: one of them does the
 * work and the others wait for it and get its result.
 *
 * The store keeps its own files next to the installations, in `.locks` and `.staging`. Ids never start with a dot, so they
 * cannot clash.
 *
 * @param directory the store directory. It is created on the first installation.
 * @param markerFileName the file that marks an installation as complete. A store that already holds installations made by
 * other code passes the name that code uses, so that those installations count as complete.
 * @param leftoverPolicy what to do with a directory that has an installation's name but no marker file.
 * @param listener receives events, for example when a caller has to wait for another installation.
 */
public class InstallationStore internal constructor(
    directory: Path,
    private val markerFileName: String,
    private val leftoverPolicy: LeftoverPolicy,
    private val listener: ProvisioningListener,
    private val move: (source: Path, target: Path) -> Unit,
    private val sleep: (millis: Long) -> Unit,
) {
    public constructor(
        directory: Path,
        markerFileName: String = DEFAULT_MARKER_FILE_NAME,
        leftoverPolicy: LeftoverPolicy = LeftoverPolicy.REPLACE,
        listener: ProvisioningListener = ProvisioningListener.None,
    ) : this(directory, markerFileName, leftoverPolicy, listener, ::atomicMove, Thread::sleep)

    /** The store directory, as an absolute path. */
    public val directory: Path = directory.toAbsolutePath().normalize()

    init {
        require(isSingleFileName(markerFileName)) { "The marker file name must be a single file name, got '$markerFileName'" }
    }

    /**
     * Returns the installation for [id] if it is complete, or null. Never waits and never installs.
     */
    public fun find(id: String): Path? = installationPath(id).takeIf(::isComplete)

    /**
     * Returns the complete installation for [id], running [produce] to make it if there is none yet.
     *
     * [produce] gets an empty staging directory and fills it, for example by unpacking an archive there. It returns the
     * directory that becomes the installation: the staging directory itself or a directory below it. A symlink to such a
     * directory works too; the directory it points to is installed. Everything else in the staging directory is deleted
     * afterwards.
     *
     * Callers that ask for the same [id] while it is being installed wait, then return the same installation without running
     * their own [produce]. Installations of different ids do not wait for each other. If [produce] throws, nothing is
     * installed, the exception reaches the caller unchanged, and the next call starts over.
     *
     * Must not be called for the same [id] from inside [produce].
     *
     * @throws InvalidDistributionException if [produce] returns something other than a directory inside the staging directory.
     * @throws LeftoverInstallationException if an incomplete directory is in the way and the policy is [LeftoverPolicy.FAIL].
     */
    public fun install(id: String, produce: (staging: StagingArea) -> Path): Path {
        val installation = installationPath(id)
        if (isComplete(installation)) return installation

        val lockFile = directory.resolve(LOCKS_DIRECTORY).resolve("$id.lock")
        Files.createDirectories(lockFile.parent)
        val lock = InstallationLock(lockFile, onWait = { listener.waitingForLock(id, lockFile) }, sleep = sleep)
        return lock.withLock {
            // whoever held the lock before may have installed it
            if (isComplete(installation)) return@withLock installation

            removeLeftover(installation)

            // Under the lock the staging directory of this id is ours alone. Anything in it is from an attempt that has ended.
            val staging = directory.resolve(STAGING_DIRECTORY).resolve(id)
            deleteRecursively(staging)
            Files.createDirectories(staging)
            try {
                val root = checkProduced(produce(StagingArea(staging)), staging, id)
                val marker = root.resolve(markerFileName)
                if (!Files.exists(marker, LinkOption.NOFOLLOW_LINKS)) Files.createFile(marker)
                publish(root, installation)
            } catch (e: Throwable) {
                // the caller has to see what went wrong, not that cleaning up after it failed as well
                try {
                    deleteRecursively(staging)
                } catch (cleanupFailure: IOException) {
                    e.addSuppressed(cleanupFailure)
                }
                throw e
            }
            try {
                deleteRecursively(staging)
            } catch (e: IOException) {
                // The installation is published. Leftovers in staging waste some disk space and are not worth failing for.
            }
            listener.installed(id, installation)
            installation
        }
    }

    private fun installationPath(id: String): Path {
        require(isSingleFileName(id) && !id.startsWith(".")) {
            "An installation id must be a single directory name that does not start with '.', got '$id'"
        }
        return directory.resolve(id)
    }

    private fun isComplete(installation: Path): Boolean = Files.exists(installation.resolve(markerFileName))

    private fun removeLeftover(installation: Path) {
        // a broken symlink counts as well
        if (!Files.exists(installation, LinkOption.NOFOLLOW_LINKS)) return
        when (leftoverPolicy) {
            LeftoverPolicy.FAIL -> throw LeftoverInstallationException(installation, markerFileName)
            LeftoverPolicy.REPLACE -> {
                deleteRecursively(installation)
                listener.leftoverRemoved(installation)
            }
        }
    }

    private fun checkProduced(produced: Path, staging: Path, id: String): Path {
        // The real path, for two reasons. A symlink must not bring in a directory from outside the staging directory.
        // And publishing renames what it is given: renaming a symlink would install the link, which dangles once staging is gone.
        val root = try {
            produced.toRealPath()
        } catch (e: IOException) {
            null
        }
        if (root == null || !Files.isDirectory(root) || !root.startsWith(staging.toRealPath())) {
            throw InvalidDistributionException(
                "The installation of '$id' must be a directory inside its staging directory '$staging', got '$produced'"
            )
        }
        return root
    }

    private fun publish(root: Path, installation: Path) {
        // Under the lock the target does not exist, so the move only fails when something holds a file inside 'root' open.
        // On Windows antivirus and indexers do that to freshly written files for a short while.
        var attempt = 1
        while (true) {
            try {
                move(root, installation)
                return
            } catch (e: AccessDeniedException) {
                if (attempt == PUBLISH_ATTEMPTS) throw e
                sleep(PUBLISH_RETRY_DELAY_MILLIS * attempt)
                attempt++
            }
        }
    }
}

/**
 * The scratch directory an installation is assembled in, see [InstallationStore.install].
 *
 * @property directory an empty directory on the same file system as the store, deleted after the installation.
 */
public class StagingArea internal constructor(public val directory: Path)

/**
 * What [InstallationStore] does with a directory that has an installation's name but no marker file. Such a directory was
 * written outside the store's protocol: by an installer that unpacks in place, by an older plugin version, or by hand.
 */
public enum class LeftoverPolicy {
    /** Delete the directory and install. Safe for a store that only [InstallationStore] writes to. */
    REPLACE,

    /** Throw [LeftoverInstallationException] and leave the directory alone. */
    FAIL,
}

private fun atomicMove(source: Path, target: Path) {
    Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
}

/** Whether [name] can be used as the name of one file or directory on every supported file system. */
internal fun isSingleFileName(name: String): Boolean =
    name.isNotBlank() && name != "." && name != ".." && name.none { it < ' ' || it in "/\\:*?\"<>|" }
