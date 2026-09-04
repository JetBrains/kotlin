// KaptConfigurationImplTest.kt
package org.jetbrains.kotlin.buildtools.internal.jvm

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

class KaptConfigurationImplTest {
    @Test
    fun `test toCompilerPluginOptions with both Apt and Stubs phases`() {
        val kaptConfig = KaptConfigurationImpl(
            kaptClasspath = listOf(Paths.get("kapt/classpath")),
            stubsOutputDir = Paths.get("stubs/output"),
            sourcesOutputDir = Paths.get("sources/output"),
            annotationProcessorsClasspath = listOf(Paths.get("processor/classpath"))
        )

        kaptConfig.withAptPhase()
        kaptConfig.withStubsPhase()

        val options = kaptConfig.toCompilerPluginOptions()
        val aptModeOption = options.find { it.key == "aptMode" }

        assertNotNull(aptModeOption)
        assertEquals("stubsAndApt", aptModeOption?.value)
    }

    @Test
    fun `test toCompilerPluginOptions with only Apt phase`() {
        val kaptConfig = KaptConfigurationImpl(
            kaptClasspath = listOf(Paths.get("kapt/classpath")),
            stubsOutputDir = Paths.get("stubs/output"),
            sourcesOutputDir = Paths.get("sources/output"),
            annotationProcessorsClasspath = listOf(Paths.get("processor/classpath"))
        )

        kaptConfig.withAptPhase()

        val options = kaptConfig.toCompilerPluginOptions()
        val aptModeOption = options.find { it.key == "aptMode" }

        assertNotNull(aptModeOption)
        assertEquals("apt", aptModeOption?.value)
    }

    @Test
    fun `test toCompilerPluginOptions with only Stubs phase`() {
        val kaptConfig = KaptConfigurationImpl(
            kaptClasspath = listOf(Paths.get("kapt/classpath")),
            stubsOutputDir = Paths.get("stubs/output"),
            sourcesOutputDir = Paths.get("sources/output"),
            annotationProcessorsClasspath = listOf(Paths.get("processor/classpath"))
        )

        kaptConfig.withStubsPhase()

        val options = kaptConfig.toCompilerPluginOptions()
        val aptModeOption = options.find { it.key == "aptMode" }

        assertNotNull(aptModeOption)
        assertEquals("stubs", aptModeOption?.value)
    }

    @Test
    fun `test toCompilerPluginOptions throws error for missing Apt and Stubs phases`() {
        val kaptConfig = KaptConfigurationImpl(
            kaptClasspath = listOf(Paths.get("kapt/classpath")),
            stubsOutputDir = Paths.get("stubs/output"),
            sourcesOutputDir = Paths.get("sources/output"),
            annotationProcessorsClasspath = listOf(Paths.get("processor/classpath"))
        )

        val exception = assertThrows<IllegalStateException> {
            kaptConfig.toCompilerPluginOptions()
        }

        assertEquals("At least one of apt or stubs phase required.", exception.message)
    }

    @Test
    fun `test toCompilerPluginOptions includes options from iterator`() {
        val kaptConfig = KaptConfigurationImpl(
            kaptClasspath = listOf(Paths.get("kapt/classpath")),
            stubsOutputDir = Paths.get("stubs/output"),
            sourcesOutputDir = Paths.get("sources/output"),
            annotationProcessorsClasspath = listOf(Paths.get("processor/classpath"))
        )

        kaptConfig.withAptPhase()
        kaptConfig.set(KaptConfigurationImpl.VERBOSE, true)

        val options = kaptConfig.toCompilerPluginOptions()
        val verboseOption = options.find { it.key == KaptConfigurationImpl.VERBOSE.id }

        assertNotNull(verboseOption)
        assertEquals("true", verboseOption?.value)
    }

    @Test
    fun `test toCompilerPluginOptions handles Path option correctly`() {
        val kaptConfig = KaptConfigurationImpl(
            kaptClasspath = listOf(Paths.get("kapt/classpath")),
            stubsOutputDir = Paths.get("stubs/output"),
            sourcesOutputDir = Paths.get("sources/output"),
            annotationProcessorsClasspath = listOf(Paths.get("processor/classpath"))
        )

        kaptConfig.withAptPhase()
        kaptConfig.set(KaptConfigurationImpl.SOURCE_OUTPUT_DIR, Paths.get("custom/source/output"))

        val options = kaptConfig.toCompilerPluginOptions()
        val sourceOutputDirOption = options.find { it.key == KaptConfigurationImpl.SOURCE_OUTPUT_DIR.id }

        assertNotNull(sourceOutputDirOption)
        assertEquals(Paths.get("custom/source/output").absolutePathString(), sourceOutputDirOption?.value)
    }

    @Test
    fun `test toCompilerPluginOptions includes JAVAC_OPTIONS`() {
        val kaptConfig = KaptConfigurationImpl(
            kaptClasspath = listOf(Paths.get("kapt/classpath")),
            stubsOutputDir = Paths.get("stubs/output"),
            sourcesOutputDir = Paths.get("sources/output"),
            annotationProcessorsClasspath = listOf(Paths.get("processor/classpath"))
        )

        kaptConfig.withAptPhase()
        kaptConfig.set(KaptConfigurationImpl.JAVAC_OPTIONS, mapOf("source" to "1.8", "target" to "1.8"))

        val options = kaptConfig.toCompilerPluginOptions()
        val javacOptionsOption = options.filter { it.key == KaptConfigurationImpl.JAVAC_OPTIONS.id }

        assertNotNull(javacOptionsOption)
        assertEquals("source=1.8,target=1.8", javacOptionsOption.joinToString(",") { it.value })
    }
}
