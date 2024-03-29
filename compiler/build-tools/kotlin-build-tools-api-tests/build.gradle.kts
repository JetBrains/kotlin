import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

plugins {
    kotlin("jvm")
    `jvm-test-suite`
    id("test-symlink-transformation")
}

dependencies {
    api(kotlinStdlib())
    compileOnly(project(":kotlin-tooling-core")) // to reuse `KotlinToolingVersion`
    compileOnly(project(":compiler:build-tools:kotlin-build-tools-api"))
    api(projectTests(":compiler:test-infrastructure-utils")) // for `@TestDataPath`/`@TestMetadata`

    api(platform(libs.junit.bom))
    compileOnly(libs.junit.jupiter.engine)
    compileOnly(libs.junit.jupiter.params)
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
        optIn.add("kotlin.ExperimentalStdlibApi")
        optIn.add("kotlin.io.path.ExperimentalPathApi")
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

val compatibilityTestsVersions = listOf(
    BuildToolsVersion(KotlinToolingVersion(project.version.toString()), isCurrent = true),
    BuildToolsVersion(KotlinToolingVersion(1, 9, 20, null)),
)

class BuildToolsVersion(val version: KotlinToolingVersion, val isCurrent: Boolean = false) {
    override fun toString() = if (isCurrent) "Snapshot" else version.toString()
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

fun SourceSet.configureCompatibilitySourceDirectories() {
    java.setSrcDirs(
        listOf(
            layout.projectDirectory.dir("src/testCompatibility/java"),
        )
    )
    kotlin.setSrcDirs(
        listOf(
            layout.projectDirectory.dir("src/testCompatibility/java"),
            layout.projectDirectory.dir("src/testCompatibility/kotlin"),
        )
    )
    resources.setSrcDirs(
        listOf(
            layout.projectDirectory.dir("src/testCompatibility/resources"),
        )
    )
}

// just add a new test suit name here and that's it
val businessLogicTestSuits = setOf(
    "testExample",
    "testEscapableCharacters",
)

testing {
    suites {
        for (suit in businessLogicTestSuits) {
            register<JvmTestSuite>(suit)
        }

        var configuredIdeaSourceSets = false
        for (implVersion in compatibilityTestsVersions) {
            register<JvmTestSuite>("testCompatibility${implVersion}") {
                if (!kotlinBuildProperties.isInIdeaSync || !configuredIdeaSourceSets) {
                    sources.configureCompatibilitySourceDirectories()
                    configuredIdeaSourceSets = true
                }
                dependencies {
                    if (implVersion.isCurrent) {
                        runtimeOnly(project(":compiler:build-tools:kotlin-build-tools-impl"))
                    } else {
                        runtimeOnly("org.jetbrains.kotlin:kotlin-build-tools-impl:${implVersion}")
                    }
                }
                targets.all {
                    projectTest(taskName = testTask.name, jUnitMode = JUnitMode.JUnit5) {
                        ensureExecutedAgainstExpectedBuildToolsImplVersion(implVersion)
                    }
                }
            }
        }

        withType<JvmTestSuite>().configureEach configureSuit@{
            val isRegular = this@configureSuit.name in businessLogicTestSuits
            dependencies {
                useJUnitJupiter(libs.versions.junit5.get())

                compileOnly(project()) // propagate stdlib from the main dependencies for compilation, the runtime dependency provides the actual required version
                implementation(project()) {
                    isTransitive = false
                }
                implementation(project(":kotlin-tooling-core"))
                compileOnly(project(":compiler:build-tools:kotlin-build-tools-api"))
                if (isRegular) {
                    runtimeOnly(project(":compiler:build-tools:kotlin-build-tools-impl"))
                }
            }

            targets.all {
                projectTest(taskName = testTask.name, jUnitMode = JUnitMode.JUnit5) {
                    systemProperty("kotlin.build-tools-api.log.level", "DEBUG")
                }
            }
        }

        named<JvmTestSuite>("testEscapableCharacters") {
            configurations.named(sources.runtimeClasspathConfigurationName) {
                testSymlinkTransformation.resolveAgainstSymlinkedArtifacts(this)
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.matching { it.name != "testExample" }) // do not run example tests by default
}