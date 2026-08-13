
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("common-configuration")
    id("test-federation-convention")
    kotlin("jvm")
}

configureKotlinCompileTasksGradleCompatibility()

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$coreDepsVersion")
}

// `getJdkClassesClassLoader` calls the JDK 9+ `ClassLoader.getPlatformClassLoader()` behind a runtime
// version check, so the API must not be restricted to the Java 8 target.
configureJvmToolchain(
    JdkMajorVersion.JDK_21_0,
    target = JdkMajorVersion.JDK_1_8,
    restrictApiToTarget = false,
)

kotlin {
    explicitApi()
    @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
    compilerVersion.set(libs.versions.kotlin.`for`.gradle.plugins.compilation)
}
