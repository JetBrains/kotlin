plugins {
    kotlin("jvm")
    `jvm-test-suite`
    id("test-symlink-transformation")
    id("project-tests-convention")
    id("test-inputs-check")
}

val btaApiVersion = "2.3.0"

val buildToolsApiImpl = configurations.dependencyScope("buildToolsApiImpl")
val buildToolsApiImplResolvable = configurations.resolvable("buildToolsApiImplResolvable") {
    extendsFrom(buildToolsApiImpl.get())
}

dependencies {
    api(kotlinStdlib())
    compileOnly(project(":kotlin-tooling-core")) // to reuse `KotlinToolingVersion`
    compileOnly("org.jetbrains.kotlin:kotlin-build-tools-api:$btaApiVersion")
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


val COMPILER_CLASSPATH_PROPERTY = "kotlin.build-tools-api.test.compilerClasspath"

fun JvmTestSuite.addSnapshotBuildToolsImpl() {
    targets.all {
        testTask.configure {
            addClasspathProperty(buildToolsApiImplResolvable.get(), COMPILER_CLASSPATH_PROPERTY)
        }
    }
}

testing {
    suites {
        register<JvmTestSuite>("testCompatibility") {
            addSnapshotBuildToolsImpl()
            targets.all {
                projectTests {
                    testTask(taskName = testTask.name, jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = false) {
                        systemProperty("kotlin.build-tools-api.log.level", "DEBUG")
                        systemProperty(
                            "kotlin.daemon.custom.run.files.path.for.tests",
                            "build/daemon"
                        )
                        extensions.configure<TestInputsCheckExtension> {
                            extraPermissions.set(
                                listOfNotNull(
                                    "permission java.net.SocketPermission \"localhost\", \"connect,resolve,accept\";",
                                    "permission java.util.PropertyPermission \"java.rmi.server.hostname\", \"write\";",

                                    // paths below are not expected to exist,
                                    // these are here to pass implicit `exists()` checks in the Kotlin compiler
                                    "permission java.io.FilePermission \"<no_path>/lib\", \"read\";",
                                    "permission java.io.FilePermission \"./kotlin-scripting-compiler.jar\", \"read\";",
                                    "permission java.io.FilePermission \"./kotlin-scripting-compiler-impl.jar\", \"read\";",
                                    "permission java.io.FilePermission \"./kotlin-scripting-common.jar\", \"read\";",
                                    "permission java.io.FilePermission \"./kotlin-scripting-jvm.jar\", \"read\";"
                                )
                            )
                        }
                    }
                }
            }
        }


        withType<JvmTestSuite>().configureEach configureSuit@{
            dependencies {
                useJUnitJupiter(libs.versions.junit5.get())
                runtimeOnly(libs.junit.platform.launcher)

                implementation(project())
                implementation(project(":kotlin-tooling-core"))
                implementation(project(":compiler:test-security-manager"))
                implementation("org.jetbrains.kotlin:kotlin-build-tools-api:$btaApiVersion")
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites)
}
