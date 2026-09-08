
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("common-configuration")
    kotlin("jvm")
}

configureKotlinCompileTasksGradleCompatibility()

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$coreDepsVersion")
}

jvmToolchains {
    jdkVersion = JdkMajorVersion.JDK_21_0
    targetBytecodeVersion = JdkMajorVersion.JDK_1_8

    // `classLoaderUtils.kt` references the JDK 9+ API under runtime check.
    // So the modern JDK API should be available for compile tasks.
    jdkApiVersion = jdkVersion
}

kotlin {
    explicitApi()
    @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
    compilerVersion.set(libs.versions.kotlin.`for`.gradle.plugins.compilation)
}
