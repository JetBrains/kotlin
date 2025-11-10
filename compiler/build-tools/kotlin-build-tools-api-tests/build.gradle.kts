import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

plugins {
    kotlin("jvm")
    `jvm-test-suite`
    id("test-symlink-transformation")
    id("project-tests-convention")
}

dependencies {
    implementation(kotlinStdlib())
    compileOnly(project(":kotlin-tooling-core")) // to reuse `KotlinToolingVersion`
    compileOnly(project(":compiler:build-tools:kotlin-build-tools-api"))
    compileOnly(project(":compiler:build-tools:kotlin-build-tools-compat"))
    implementation(testFixtures(project(":compiler:test-infrastructure-utils"))) // for `@TestDataPath`/`@TestMetadata`

    implementation(platform(libs.junit.bom))
    compileOnly(libs.junit.jupiter.engine)
    compileOnly(libs.junit.jupiter.params)
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
        optIn.add("kotlin.ExperimentalStdlibApi")
        optIn.add("kotlin.io.path.ExperimentalPathApi")
    }
}

val compatibilityTestsVersions = listOf(
    BuildToolsVersion(KotlinToolingVersion(project.version.toString()), isCurrent = true),
    BuildToolsVersion(KotlinToolingVersion(2, 1, 20, null)),
    BuildToolsVersion(KotlinToolingVersion(2, 0, 21, null)),
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

fun SourceSet.configureCompatibilitySourceDirectories(testSuiteName: String) {
    java.setSrcDirs(
        listOf(
            layout.projectDirectory.dir("src/$testSuiteName/java"),
        )
    )
    kotlin.setSrcDirs(
        listOf(
            layout.projectDirectory.dir("src/$testSuiteName/java"),
            layout.projectDirectory.dir("src/$testSuiteName/kotlin"),
        )
    )
    resources.setSrcDirs(
        listOf(
            layout.projectDirectory.dir("src/$testSuiteName/resources"),
        )
    )
}

// just add a new test suit name here and that's it
val businessLogicTestSuits = setOf(
    "testExample",
    "testEscapableCharacters",
    "testInputChangesTracking",
    "testCrossModuleIncrementalChanges",
    "testFirRunner",
    "testCriToolchain",
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
                    sources.configureCompatibilitySourceDirectories("testCompatibility")
                }
                dependencies {
                    runtimeOnly(project(":compiler:build-tools:kotlin-build-tools-compat"))
                    if (implVersion.isCurrent) {
                        runtimeOnly(project(":compiler:build-tools:kotlin-build-tools-impl"))
                    } else {
                        runtimeOnly("org.jetbrains.kotlin:kotlin-build-tools-impl:${implVersion}")
                    }
                }
                targets.all {
                    projectTests {
                        testTask(taskName = testTask.name, jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = false) {
                            ensureExecutedAgainstExpectedBuildToolsImplVersion(implVersion)
                            systemProperty("kotlin.build-tools-api.log.level", "DEBUG")
                        }
                    }
                }
            }

            // in the `testIsolatedCompiler` tests we will need classpaths for the compiler and matching stdlib at runtime
            configurations.create("isolatedCompilerClasspath$implVersion")
            configurations.create("isolatedCompilerStdlib$implVersion")
            dependencies {
                "isolatedCompilerClasspath$implVersion"(project(":compiler:build-tools:kotlin-build-tools-compat"))
                if (implVersion.isCurrent) {
                    "isolatedCompilerClasspath$implVersion"(project(":compiler:build-tools:kotlin-build-tools-impl"))
                    "isolatedCompilerStdlib$implVersion"(project(":kotlin-stdlib"))
                } else {
                    "isolatedCompilerClasspath$implVersion"("org.jetbrains.kotlin:kotlin-build-tools-impl:${implVersion}")
                    "isolatedCompilerStdlib$implVersion"("org.jetbrains.kotlin:kotlin-stdlib:${implVersion}")
                }
            }
            register<JvmTestSuite>("testIsolatedCompiler${implVersion}") {
                if (!kotlinBuildProperties.isInIdeaSync || !configuredIdeaSourceSets) {
                    sources.configureCompatibilitySourceDirectories("testIsolatedCompiler")
                }

                targets.all {
                    projectTests {
                        testTask(taskName = testTask.name, jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = false) {
                            systemProperty("kotlin.build-tools-api.log.level", "DEBUG")
                            systemProperty(
                                "kotlin.build-tools-api.test.compilerClasspath",
                                configurations.named("isolatedCompilerClasspath$implVersion").get().asPath
                            )
                            systemProperty(
                                "kotlin.build-tools-api.test.stdlibClasspath",
                                configurations.named("isolatedCompilerStdlib$implVersion").get().asPath
                            )
                        }
                    }
                }
            }
            configuredIdeaSourceSets = true
        }

        withType<JvmTestSuite>().configureEach configureSuit@{
            val isRegular = this@configureSuit.name in businessLogicTestSuits
            val isIsolatedClasspath = this@configureSuit.name.startsWith("testIsolatedCompiler")
            dependencies {
                useJUnitJupiter(libs.versions.junit5.get())

                compileOnly(project()) // propagate stdlib from the main dependencies for compilation, the runtime dependency provides the actual required version
                implementation(project()) {
                    // for the "testIsolatedCompiler" test suite, we don't bring in the build-tools-impl dependency,
                    // so we don't have stdlib and other deps either. We need to bring them in explicitly.
                    isTransitive = isIsolatedClasspath
                }
                implementation(project(":kotlin-tooling-core"))
                implementation(project(":compiler:build-tools:kotlin-build-tools-api"))
                if (!isIsolatedClasspath) {
                    runtimeOnly(project(":compiler:build-tools:kotlin-build-tools-compat"))
                    if (isRegular) {
                        runtimeOnly(project(":compiler:build-tools:kotlin-build-tools-impl"))
                        runtimeOnly(project(":compiler:build-tools:kotlin-build-tools-cri-impl"))
                    }
                }
            }

            targets.all {
                if (businessLogicTestSuits.any { testTask.name.startsWith(it) }) {
                    projectTests {
                        testTask(taskName = testTask.name, jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = false) {
                            systemProperty("kotlin.build-tools-api.log.level", "DEBUG")
                        }
                    }
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
