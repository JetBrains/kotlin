/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.klib

import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMON_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_SOURCES_DIR
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.idea.configuration.klib.KlibInfoProvider.Origin.*
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.TargetSupportException
import org.jetbrains.kotlin.library.KLIB_MANIFEST_FILE_NAME
import org.jetbrains.kotlin.library.KLIB_PROPERTY_UNIQUE_NAME
import java.io.File
import java.io.IOException
import java.nio.file.Files.*
import java.nio.file.Path
import java.util.*

class KlibInfoProvider(
    kotlinNativeHome: File,
    nativeDistributionCommonizedLibsDir: File? = null
) {
    private enum class Origin {
        NATIVE_DISTRIBUTION,
        NATIVE_DISTRIBUTION_COMMONIZED,
        // TODO: add other origins as necessary
    }

    private class ResultWrapper<T>(val result: T)

    private val kotlinNativePath = kotlinNativeHome.toPath()
    private val nativeDistributionCommonizedLibsPath = nativeDistributionCommonizedLibsDir?.toPath()

    private val hostManager by lazy {
        HostManager(
            distribution = Distribution(konanHomeOverride = kotlinNativeHome.path),
            experimental = true
        )
    }

    private val nativeDistributionLibrarySourceFiles by lazy {
        kotlinNativeHome.resolve(KONAN_DISTRIBUTION_SOURCES_DIR)
            .takeIf { it.isDirectory }
            ?.walkTopDown()
            ?.maxDepth(1)
            ?.filter { it.isFile && it.name.endsWith(".zip") }
            ?.toList()
            ?: emptyList()
    }

    private val cachedProperties = mutableMapOf<Path, Properties>()

    fun getKlibInfo(libraryFile: File): KlibInfo? {
        val libraryPath = libraryFile.toPath()

        val origin = detectOrigin(libraryPath) ?: return null
        val manifest = loadProperties(libraryPath.resolve(KLIB_MANIFEST_FILE_NAME))

        val name = manifest.getProperty(KLIB_PROPERTY_UNIQUE_NAME) ?: return null
        val target = (detectTarget(libraryPath) ?: return null).result

        return when (origin) {
            NATIVE_DISTRIBUTION -> {
                val sourcePaths = if (target == null) getLibrarySourcesFromDistribution(name) else emptyList()
                NativeDistributionKlibInfo(libraryFile, sourcePaths, name, target)
            }

            NATIVE_DISTRIBUTION_COMMONIZED -> {
                val commonizedTargets = (detectCommonizedTargets(libraryPath, target) ?: return null).result
                NativeDistributionCommonizedKlibInfo(libraryFile, emptyList(), name, target, commonizedTargets)
            }
        }
    }

    private fun detectOrigin(libraryPath: Path): Origin? = when {
        libraryPath.startsWith(kotlinNativePath) -> NATIVE_DISTRIBUTION
        nativeDistributionCommonizedLibsPath?.let { libraryPath.startsWith(it) } == true -> NATIVE_DISTRIBUTION_COMMONIZED
        else -> null
    }

    private fun loadProperties(propertiesPath: Path): Properties = cachedProperties.computeIfAbsent(propertiesPath) {
        val properties = Properties()

        if (isRegularFile(propertiesPath)) {
            try {
                newInputStream(propertiesPath).use { properties.load(it) }
            } catch (_: IOException) {
                // do nothing
            }
        }

        properties
    }

    private fun detectTarget(libraryPath: Path): ResultWrapper<KonanTarget?>? {
        if (libraryPath.nameCount < 3) return null

        val parentDirName = libraryPath.getName(libraryPath.nameCount - 2).toString()
        if (parentDirName == KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
            return ResultWrapper(/* common */ null)
        else {
            val target = parentDirName.safeToTarget() ?: return null
            val grandParentDirName = libraryPath.getName(libraryPath.nameCount - 3).toString()
            return if (grandParentDirName == KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR)
                ResultWrapper(target)
            else
                null
        }
    }

    private fun detectCommonizedTargets(libraryPath: Path, ownTarget: KonanTarget?): ResultWrapper<Set<KonanTarget>>? {
        if (libraryPath.nameCount < 4) return null

        val additionalOffset = if (ownTarget == null) /* common */ 0 else 1
        val basePath = libraryPath.subpath(0, libraryPath.nameCount - 2 - additionalOffset)

        val props = loadProperties(basePath.resolve(COMMONIZED_PROPERTIES_FILE_NAME))
        val rawTargets = props.getProperty(COMMONIZED_TARGETS)?.split(' ') ?: return null

        val targets = rawTargets.mapNotNullTo(mutableSetOf()) { it.safeToTarget() }
        if (targets.isEmpty() || targets.size != rawTargets.size || ownTarget !in targets) return null

        return ResultWrapper(targets)
    }

    private fun getLibrarySourcesFromDistribution(libraryName: String): Collection<File> {
        val nameFilter: (String) -> Boolean = if (libraryName == KONAN_STDLIB_NAME) {
            // stdlib is a special case
            { it.startsWith("kotlin-stdlib") || it.startsWith("kotlin-test") }
        } else {
            { it.startsWith(libraryName) }
        }

        return nativeDistributionLibrarySourceFiles.filter { nameFilter(it.name) }
    }

    private fun String.safeToTarget(): KonanTarget? = try {
        hostManager.targetByName(this)
    } catch (_: TargetSupportException) {
        null
    }

    private companion object {
        const val COMMONIZED_PROPERTIES_FILE_NAME = ".commonized"
        const val COMMONIZED_TARGETS = "targets"
    }
}
