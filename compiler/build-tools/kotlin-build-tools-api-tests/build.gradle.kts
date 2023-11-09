import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

plugins {
    kotlin("jvm")
    id("jps-compatible")
    `jvm-test-suite`
}

dependencies {
    api(kotlinStdlib())
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
        optIn.add("kotlin.ExperimentalStdlibApi")
        optIn.add("kotlin.io.path.ExperimentalPathApi")
    }
}

class ApiPair(val testName: String, val apiVersion: BuildToolsVersion, val implVersion: BuildToolsVersion)

val testMatrix = listOf(
    ApiPair(
        "testDefaultToDefault",
        BuildToolsVersion(KotlinToolingVersion(project.version.toString()), isCurrent = true),
        BuildToolsVersion(KotlinToolingVersion(project.version.toString()), isCurrent = true),
    ),
    ApiPair(
        "test1.9.20ToDefault",
        BuildToolsVersion(KotlinToolingVersion(1, 9, 20, null)),
        BuildToolsVersion(KotlinToolingVersion(project.version.toString()), isCurrent = true),
    ),
    ApiPair(
        "testDefaultTo1.9.20",
        BuildToolsVersion(KotlinToolingVersion(project.version.toString()), isCurrent = true),
        BuildToolsVersion(KotlinToolingVersion(1, 9, 20, null)),
    ),
)

val SourceSet.kotlinCompileTask
    get() = tasks.named<KotlinCompile>("compile${name.capitalized()}Kotlin")

class BuildToolsVersion(val version: KotlinToolingVersion, val isCurrent: Boolean = false) {
    override fun toString() = version.toString()
}

fun KotlinCompile.ensureCompiledAgainstExpectedBuildToolsApiVersion(version: BuildToolsVersion) {
    if (version.isCurrent) return
    // the check is required for the case when Gradle substitutes external dependencies with project ones
    doFirst {
        check(libraries.any { "kotlin-build-tools-api-${version}" in it.name }) {
            "compilation classpath must contain kotlin-build-tools-api:$version"
        }
    }
}

fun Test.ensureExecutedAgainstExpectedBuildToolsImplVersion(version: BuildToolsVersion) {
    if (version.isCurrent) return
    // the check is required for the case when Gradle substitutes external dependencies with project ones
    doFirst {
        check(classpath.any { "kotlin-build-tools-impl-${version}" in it.name }) {
            "runtime classpath must contain kotlin-build-tools-impl:$version"
        }
    }
}

fun SourceSet.configureApiVersionSourceDirectories(apiVersion: BuildToolsVersion) {
    java.setSrcDirs(
        listOf(
            layout.projectDirectory.dir("src/testCommon/java"),
            layout.projectDirectory.dir("src/testApi$apiVersion/java"),
        )
    )
    kotlin.setSrcDirs(
        listOf(
            layout.projectDirectory.dir("src/testCommon/java"),
            layout.projectDirectory.dir("src/testApi$apiVersion/java"),
            layout.projectDirectory.dir("src/testCommon/kotlin"),
            layout.projectDirectory.dir("src/testApi$apiVersion/kotlin"),
        )
    )
    resources.setSrcDirs(
        listOf(
            layout.projectDirectory.dir("src/testCommon/resources"),
            layout.projectDirectory.dir("src/testApi$apiVersion/resources"),
        )
    )
}

testing {
    suites {
        for (apiPair in testMatrix) {
            register<JvmTestSuite>(apiPair.testName) {
                sources.configureApiVersionSourceDirectories(apiPair.apiVersion)
                dependencies {
                    useJUnitJupiter(libs.versions.junit5.get())

                    compileOnly(project()) // propagate stdlib from the main dependencies for compilation,
                    // the runtime dependency provides the actual required version
                    implementation(project(":kotlin-tooling-core")) // to reuse `KotlinToolingVersion`

                    if (apiPair.apiVersion.isCurrent) {
                        compileOnly(project(":compiler:build-tools:kotlin-build-tools-api"))
                    } else {
                        compileOnly("org.jetbrains.kotlin:kotlin-build-tools-api:${apiPair.apiVersion}")
                    }
                    sources.kotlinCompileTask.configure {
                        ensureCompiledAgainstExpectedBuildToolsApiVersion(apiPair.apiVersion)
                    }

                    if (apiPair.implVersion.isCurrent) {
                        runtimeOnly(project(":compiler:build-tools:kotlin-build-tools-impl"))
                    } else {
                        runtimeOnly("org.jetbrains.kotlin:kotlin-build-tools-impl:${apiPair.implVersion}")
                    }
                }

                targets.all {
                    testTask.configure {
                        ensureExecutedAgainstExpectedBuildToolsImplVersion(apiPair.implVersion)
                    }
                    projectTest(taskName = testTask.name, jUnitMode = JUnitMode.JUnit5) {
                        systemProperty("kotlin.build-tools-api.log.level", "DEBUG")
                        systemProperty("kotlin.build-tools-api.impl-version", apiPair.implVersion.toString())
                    }
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites)
}