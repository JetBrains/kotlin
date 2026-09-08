/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.buildtools.internal.KotlinBuildToolsInternalJdkUtils
import org.jetbrains.kotlin.buildtools.internal.getJdkClassesClassLoader
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources

@OptIn(KotlinBuildToolsInternalJdkUtils::class)
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class JavaVersionTest {
    @DisplayName("Unstable Java versions use the platform classloader")
    @Test
    fun testUnstableJavaVersionsUsePlatformClassLoader() {
        assumeTrue(isCurrentRuntimeJava9OrNewer(), "Platform classloader exists only since Java 9")

        for (javaVersion in listOf("28-ea", "28-beta")) {
            withJavaVersion(javaVersion) {
                assertSame(
                    platformClassLoader(),
                    getJdkClassesClassLoader(),
                    "java.version=$javaVersion"
                )
            }
        }
    }

    @DisplayName("Modern Java versions use the platform classloader")
    @Test
    fun testModernJavaVersionsUsePlatformClassLoader() {
        assumeTrue(isCurrentRuntimeJava9OrNewer(), "Platform classloader exists only since Java 9")

        withJavaVersion("17.0.11") {
            assertSame(platformClassLoader(), getJdkClassesClassLoader())
        }
    }

    @DisplayName("Legacy and malformed Java versions use the bootstrap classloader")
    @Test
    fun testLegacyAndMalformedJavaVersionsUseBootstrapClassLoader() {
        for (javaVersion in listOf("1.8.0_412", "bad-version")) {
            withJavaVersion(javaVersion) {
                assertNull(getJdkClassesClassLoader(), "java.version=$javaVersion")
            }
        }
    }

    private fun withJavaVersion(javaVersion: String, action: () -> Unit) {
        val originalJavaVersion = System.getProperty("java.version")
        try {
            System.setProperty("java.version", javaVersion)
            action()
        } finally {
            if (originalJavaVersion == null) {
                System.clearProperty("java.version")
            } else {
                System.setProperty("java.version", originalJavaVersion)
            }
        }
    }

    private fun platformClassLoader(): ClassLoader =
        ClassLoader::class.java.getMethod("getPlatformClassLoader").invoke(null) as ClassLoader

    private fun isCurrentRuntimeJava9OrNewer(): Boolean {
        val specificationVersion = System.getProperty("java.specification.version")
        val majorVersion = if (specificationVersion.startsWith("1.")) {
            specificationVersion.substringAfter('.').toIntOrNull()
        } else {
            specificationVersion.substringBefore('.').toIntOrNull()
        }
        return (majorVersion ?: 8) > 8
    }
}
