/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

/**
 * Checks if the provided version is a legacy Java version.
 * The new versioning scheme has been in place since Java 9.
 */
private fun isLegacyJavaVersion(versionParts: List<String>) = "1" == versionParts[0] && versionParts.size > 1

private const val MINIMAL_SUPPORTED_JDK_VERSION: Int = 8

/**
 * Returns the major version of the Java platform.
 *
 * If the provided version string is a legacy Java version (e.g., "1.8.0_412"), the method returns
 * the value of the "minor" version part as the major version number.
 * For newer version strings (e.g., "17.0.11"), the method returns the value of the "major" version part.
 *
 * If any error occurs during the retrieval of the version string or its parsing, the method returns the
 * default value of [MINIMAL_SUPPORTED_JDK_VERSION] as the major version number.
 */
private fun getJavaMajorVersion(): Int = try {
    val versionParts = System.getProperty("java.version").split(".")
    if (isLegacyJavaVersion(versionParts)) {
        // e.g. 1.8.0_412
        versionParts[1].toInt()
    } else {
        // e.g. 17.0.11
        versionParts[0].toInt()
    }
} catch (e: Exception) {
    MINIMAL_SUPPORTED_JDK_VERSION
}

/**
 * Retrieves the [ClassLoader] for the JDK classes.
 *
 * If the Java major version is greater than 8, returns the platform [ClassLoader] introduced since Java 9.
 * Otherwise, returns null, indicating the bootstrap [ClassLoader].
 */
@KotlinBuildToolsInternalJdkUtils
public fun getJdkClassesClassLoader(): ClassLoader? {
    return if (getJavaMajorVersion() > 8) {
        ClassLoader.getPlatformClassLoader()
    } else {
        null
    }
}