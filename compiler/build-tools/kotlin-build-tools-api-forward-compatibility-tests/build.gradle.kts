import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.TemporaryTestFederationApi
import org.jetbrains.kotlin.testFederation.smokeTestConfig
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

plugins {
    id("common-configuration")
    id("test-federation-convention")
    kotlin("jvm")
    `jvm-test-suite`
    id("test-symlink-transformation")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

val buildToolsApiImpl = configurations.dependencyScope("buildToolsApiImpl")
val buildToolsApiImplResolvable = configurations.resolvable("buildToolsApiImplResolvable") {
    extendsFrom(buildToolsApiImpl.get())
}

dependencies {
    api(kotlinStdlib())
    api(project(":compiler:build-tools:kotlin-build-tools-api-forward-compatibility-tests:shared"))
    compileOnly(project(":kotlin-tooling-core")) // to reuse `KotlinToolingVersion`
    api(testFixtures(project(":compiler:test-infrastructure-utils"))) // for `@TestDataPath`/`@TestMetadata`
    api(platform(libs.junit.bom))
    compileOnly(libs.junit.jupiter.engine)
    compileOnly(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    buildToolsApiImpl(project(":compiler:build-tools:kotlin-build-tools-impl"))
    buildToolsApiImpl(project(":compiler:build-tools:kotlin-build-tools-cri-impl"))
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
        optIn.add("kotlin.ExperimentalStdlibApi")
        optIn.add("kotlin.io.path.ExperimentalPathApi")
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

val compatibilityTestsVersions = listOf(
    KotlinToolingVersion(2, 4, 0, null),
)

val KotlinToolingVersion.sourceSetName get() = "shared" + this.toString().replace(".", "_").replace("-", "_")

val COMPILER_CLASSPATH_PROPERTY = "kotlin.build-tools-api.test.compilerClasspath"

fun JvmTestSuite.ensureExecutedAgainstExpectedBuildToolsApiVersion(version: KotlinToolingVersion) {
    targets.all {
        projectTests {
            testTask.configure {
                // the check is required for the case when Gradle substitutes external dependencies with project ones
                doFirst {
                    check(
                        classpath.files.any { "kotlin-build-tools-api-${version}.jar" in it.name }
                    ) {
                        "runtime classpath must contain kotlin-build-tools-api-$version.jar but was: ${classpath.joinToString()}"
                    }
                }
            }
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

fun JvmTestSuite.addSnapshotBuildToolsImpl() {
    targets.all {
        testTask.configure {
            addClasspathProperty(buildToolsApiImplResolvable.get(), COMPILER_CLASSPATH_PROPERTY)
        }
    }
}

fun JvmTestSuite.addSpecificBuildToolsApi(version: KotlinToolingVersion) {
    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-build-tools-api:${version}")
        implementation(sourceSets.getByName(version.sourceSetName).output)
    }
}


for (apiVersion in compatibilityTestsVersions) {
    sourceSets {
        apiVersion.sourceSetName {
            kotlin.srcDir("src/shared$apiVersion/kotlin")
            resources.srcDir("src/shared$apiVersion/resources")
        }
    }
    dependencies {
        "${apiVersion.sourceSetName}CompileOnly"(project())
        "${apiVersion.sourceSetName}CompileOnly"("org.jetbrains.kotlin:kotlin-build-tools-api:${apiVersion}")
        "${apiVersion.sourceSetName}CompileOnly"(project(":kotlin-tooling-core"))
        "${apiVersion.sourceSetName}CompileOnly"(libs.junit.jupiter.engine)
        "${apiVersion.sourceSetName}CompileOnly"(libs.junit.jupiter.params)
    }
}

testing {
    suites {
        for (apiVersion in compatibilityTestsVersions) {
            register<JvmTestSuite>("testForwardCompatibility${apiVersion}") {
                addSnapshotBuildToolsImpl()
                addSpecificBuildToolsApi(apiVersion)
                sources.configureCompatibilitySourceDirectories("testForwardCompatibility$apiVersion")
                ensureExecutedAgainstExpectedBuildToolsApiVersion(apiVersion)
            }
        }
        withType<JvmTestSuite>().configureEach configureSuit@{
            dependencies {
                useJUnitJupiter(libs.versions.junit5.get())
                runtimeOnly(libs.junit.platform.launcher)

                implementation(project())
                implementation(project(":compiler:build-tools:kotlin-build-tools-api-forward-compatibility-tests:shared"))
                implementation(project(":kotlin-tooling-core"))
                implementation(project(":compiler:test-security-manager"))
                implementation(project(":compiler:arguments"))
            }
            targets.all {
                projectTests {
                    testTask(
                        taskName = testTask.name,
                        javaLauncher = JdkMajorVersion.JDK_1_8,
                        skipInLocalBuild = false
                    ) {
                        @OptIn(TemporaryTestFederationApi::class)
                        smokeTestConfig = SmokeTestConfig.RunAllTests

                        systemProperty("kotlin.build-tools-api.log.level", "DEBUG")
                        systemProperty(
                            "kotlin.daemon.custom.run.files.path.for.tests",
                            "build/daemon"
                        )
                    }
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites)
}
