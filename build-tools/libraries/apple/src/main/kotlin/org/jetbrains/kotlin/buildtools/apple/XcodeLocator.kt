/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

/**
 * Finds Xcode installations and picks the one a build should use.
 *
 * [resolve] applies, in order: an explicit [XcodeRequest.DeveloperDir], a [XcodeRequest.Version] matched against
 * [installations], `DEVELOPER_DIR` from [environment], and finally `xcode-select -p`.
 *
 * [installations] scans [searchDirectories] for `*.app` bundles, adds the selected Xcode and, when [useSpotlight] is
 * set, every bundle Spotlight knows with the identifier `com.apple.dt.Xcode`. Symlinks are resolved, so an `Xcode.app`
 * link to `Xcode-16.4.0.app` counts once; links whose target is gone are skipped. When two bundles have the same
 * version, the one from the earlier search directory wins, so `/Applications` comes first by default, as in Bazel.
 *
 * Nothing is cached. Resolve once per build and pass the [XcodeInstallation] on.
 *
 * @param runner runs `xcode-select` and `mdfind`.
 * @param environment the environment of the build, read for `DEVELOPER_DIR`, `HOME` and `XCODES_DIRECTORY`.
 * @param searchDirectories directories whose `*.app` children are checked, in order of preference.
 * @param useSpotlight also ask Spotlight via `mdfind`. Off by default: it starts a process and is often disabled on CI.
 */
public class XcodeLocator(
    private val runner: AppleToolRunner,
    private val environment: Map<String, String>,
    private val searchDirectories: List<Path> = defaultSearchDirectories(environment),
    private val useSpotlight: Boolean = false,
) {
    /** Returns the installation for [request]. Throws [XcodeNotFoundException] when there is none. */
    public fun resolve(request: XcodeRequest = XcodeRequest.Selected): XcodeInstallation = when (request) {
        XcodeRequest.Selected -> selected()
        is XcodeRequest.DeveloperDir -> installationAt(request.path)
            ?: throw XcodeNotFoundException("'${request.path}' is not an Xcode or Command Line Tools installation")
        is XcodeRequest.Version -> findVersion(request)
    }

    private fun selected(): XcodeInstallation {
        val developerDir = environment[DEVELOPER_DIR]?.takeIf { it.isNotBlank() }
        if (developerDir != null) {
            return installationAt(Paths.get(developerDir))
                ?: throw XcodeNotFoundException("$DEVELOPER_DIR is '$developerDir', which is not an Xcode or Command Line Tools installation")
        }
        val selectedPath = xcodeSelectPath()
        return installationAt(selectedPath)
            ?: throw XcodeNotFoundException("'xcode-select -p' prints '$selectedPath', which is not an Xcode or Command Line Tools installation")
    }

    /**
     * Every Xcode.app found, highest version first. Among equal versions, bundles from earlier [searchDirectories] come
     * first, then bundles found only through Spotlight or the selection. Command Line Tools are not listed.
     */
    public fun installations(): List<XcodeInstallation> {
        val bundles = LinkedHashSet<Path>()
        searchDirectories.flatMapTo(bundles) { appBundlesIn(it) }
        if (useSpotlight) bundles.addAll(spotlightBundles())
        val selected = try {
            selected()
        } catch (_: XcodeNotFoundException) {
            null
        }
        selected?.appBundle?.let { bundles.add(it) }

        val searchRoots = searchDirectories.map { realPathOrNull(it) ?: it.toAbsolutePath().normalize() }
        fun preference(installation: XcodeInstallation): Int =
            searchRoots.indexOfFirst { installation.appBundle?.startsWith(it) == true }.takeIf { it >= 0 } ?: Int.MAX_VALUE

        return bundles
            .mapNotNull { bundle -> realPathOrNull(bundle) }
            .distinct()
            .mapNotNull { installationAt(it) }
            .filter { it.kind == XcodeKind.XCODE }
            .sortedWith(
                compareByDescending<XcodeInstallation> { it.version }
                    .thenBy { preference(it) }
                    .thenBy { it.appBundle.toString() }
            )
    }

    private fun findVersion(request: XcodeRequest.Version): XcodeInstallation {
        val all = installations()
        val match = if (request.isBuildNumber) {
            all.firstOrNull { it.buildVersion == request.spec }
        } else {
            val components = request.versionComponents.map { it.toIntOrNull() }
            all.firstOrNull { installation ->
                val version = installation.version ?: return@firstOrNull false
                listOf(version.major, version.minor, version.patch).take(components.size) == components
            }
        }
        return match ?: throw XcodeNotFoundException(buildString {
            append("No installed Xcode matches '${request.spec}'.")
            if (all.isEmpty()) {
                append(" No Xcode installations were found in ${searchDirectories.joinToString()}.")
            } else {
                append(" Found:")
                all.forEach { append("\n  ").append(it) }
            }
        })
    }

    private fun xcodeSelectPath(): Path {
        // xcode-select itself honours DEVELOPER_DIR; the caller's environment has already been checked for it.
        val invocation = AppleToolInvocation(
            XCODE_SELECT, listOf("-p"), environmentCleanup = EnvironmentCleanup(names = setOf(DEVELOPER_DIR)),
        )
        val fix = "Install Xcode, then run 'sudo xcode-select --switch <path to Xcode.app>' or set $DEVELOPER_DIR."
        val result = try {
            runner.run(invocation)
        } catch (e: AppleToolUnavailableException) {
            throw XcodeNotFoundException("No Xcode is selected: ${e.message}. $fix", e)
        }
        val path = result.stdout.lineSequence().firstOrNull()?.trim().orEmpty()
        if (result.exitCode != 0 || path.isEmpty()) {
            val reason = result.stderr.trim().ifEmpty { "exit code ${result.exitCode}" }
            throw XcodeNotFoundException("No Xcode is selected: 'xcode-select -p' failed ($reason). $fix")
        }
        return Paths.get(path)
    }

    private fun spotlightBundles(): List<Path> {
        val invocation = AppleToolInvocation(MDFIND, listOf("kMDItemCFBundleIdentifier == '$XCODE_BUNDLE_ID'"))
        val result = try {
            runner.run(invocation)
        } catch (_: AppleToolUnavailableException) {
            return emptyList()
        }
        if (result.exitCode != 0) return emptyList()
        return result.stdout.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { Paths.get(it) }
    }

    public companion object {
        private const val DEVELOPER_DIR = "DEVELOPER_DIR"
        private const val XCODE_BUNDLE_ID = "com.apple.dt.Xcode"
        private val XCODE_SELECT: Path = Paths.get("/usr/bin/xcode-select")
        private val MDFIND: Path = Paths.get("/usr/bin/mdfind")
        private val APPLICATIONS: Path = Paths.get("/Applications")

        /** `/Applications`, `$HOME/Applications` and `$XCODES_DIRECTORY` when set, read from [environment]. */
        @JvmStatic
        public fun defaultSearchDirectories(environment: Map<String, String>): List<Path> = listOfNotNull(
            APPLICATIONS,
            environment["HOME"]?.takeIf { it.isNotBlank() }?.let { Paths.get(it, "Applications") },
            environment["XCODES_DIRECTORY"]?.takeIf { it.isNotBlank() }?.let { Paths.get(it) },
        ).distinct()

        /**
         * Describes the installation at [path]: an Xcode bundle, its `Contents/Developer`, or a Command Line Tools root.
         * Returns `null` when [path] is none of these.
         *
         * A directory counts as Xcode when it has `Contents/version.plist` and `Contents/Developer/usr/bin/xcodebuild`.
         * The bundle name does not matter, so provisioned copies such as `xcode_16.4_16F6` are recognised too.
         *
         * Symlinks are resolved first, so a link to a developer dir is described by its target.
         */
        @JvmStatic
        public fun installationAt(path: Path): XcodeInstallation? {
            val real = realPathOrNull(path) ?: return null
            val bundle: Path?
            val developerDir: Path
            if (real.resolve("Contents/Developer").isDirectory()) {
                bundle = real
                developerDir = real.resolve("Contents/Developer")
            } else {
                developerDir = real
                bundle = real.parent?.takeIf { real.name == "Developer" && it.name == "Contents" }?.parent
            }

            if (bundle == null) {
                return if (developerDir.resolve("usr/bin").isDirectory()) {
                    XcodeInstallation(developerDir, null, XcodeKind.COMMAND_LINE_TOOLS, version = null, buildVersion = null)
                } else {
                    null
                }
            }

            if (!developerDir.resolve("usr/bin/xcodebuild").exists()) return null
            val plist = try {
                VersionPlist.read(bundle.resolve("Contents").resolve(VERSION_PLIST))
            } catch (_: IOException) {
                return null
            } catch (_: VersionPlist.MalformedPlistException) {
                return null
            }
            return XcodeInstallation(
                developerDir = developerDir,
                appBundle = bundle,
                kind = XcodeKind.XCODE,
                version = plist["CFBundleShortVersionString"]?.let { XcodeVersion.parse(it) },
                buildVersion = plist["ProductBuildVersion"]?.trim()?.takeIf { it.isNotEmpty() },
            )
        }

        private const val VERSION_PLIST = "version.plist"

        private fun appBundlesIn(directory: Path): List<Path> {
            if (!directory.isDirectory()) return emptyList()
            return try {
                Files.newDirectoryStream(directory, "*.app").use { stream ->
                    stream.filter { it.isDirectory() }.sortedBy { it.toString() }
                }
            } catch (_: IOException) {
                emptyList()
            }
        }

        private fun realPathOrNull(path: Path): Path? = try {
            path.toRealPath()
        } catch (_: IOException) {
            null
        }
    }
}
