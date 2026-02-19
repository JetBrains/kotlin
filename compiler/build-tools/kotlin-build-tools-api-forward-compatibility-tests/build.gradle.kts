import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.project

plugins {
    kotlin("jvm")
    `jvm-test-suite`
    id("test-symlink-transformation")
}

val buildToolsApiImpl = configurations.dependencyScope("buildToolsApiImpl")
val buildToolsApiImplResolvable = configurations.resolvable("buildToolsApiImplResolvable") {
    extendsFrom(buildToolsApiImpl.get())
}

dependencies {
    api(kotlinStdlib())
    compileOnly(project(":kotlin-tooling-core")) // to reuse `KotlinToolingVersion`
    compileOnly("org.jetbrains.kotlin:kotlin-build-tools-api:2.3.0")
    compileOnly(project(":compiler:build-tools:kotlin-build-tools-compat")) {
        isTransitive = false
    }
    api(testFixtures(project(":compiler:test-infrastructure-utils"))) // for `@TestDataPath`/`@TestMetadata`
    api(platform(libs.junit.bom))
    compileOnly(libs.junit.jupiter.engine)
    compileOnly(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    buildToolsApiImpl(project(":compiler:build-tools:kotlin-build-tools-compat"))
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
                testTask.configure {
                    systemProperty("kotlin.build-tools-api.log.level", "DEBUG")
                }
            }
        }


        withType<JvmTestSuite>().configureEach configureSuit@{
            dependencies {
                useJUnitJupiter(libs.versions.junit5.get())
                runtimeOnly(libs.junit.platform.launcher)

                implementation(project())
                implementation(project(":kotlin-tooling-core"))
                implementation("org.jetbrains.kotlin:kotlin-build-tools-api:2.3.0")
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites)
}
