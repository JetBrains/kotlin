import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.build.foreign.registerForeignClassUsageTasks
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("multiplatform")
    id("test-inputs-check")
    id("binaryen-configuration")
    id("nodejs-configuration")
    id("d8-configuration")
    id("kotlin-git.gradle-build-conventions.foreign-class-usage-checker")
}

kotlin {
    explicitApi()

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()

    jvm()

    val buildFeatures = serviceOf<BuildFeatures>()
    wasmJs {
        if (!buildFeatures.isolatedProjects.active.get()) {
            browser()
            nodejs()
            d8()
        }
    }

    sourceSets {
        getByName("commonMain") {
            dependencies {
                api(kotlinStdlib())
            }
            kotlin {
                srcDir("common/src")
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(libs.junit.jupiter.api)
                runtimeOnly(libs.junit.jupiter.engine)
                runtimeOnly(libs.junit.platform.launcher)
                implementation(kotlinTest("junit5"))
                implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect"))
            }
            kotlin {
                srcDir("jvm/test")
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

registerForeignClassUsageTasks {
    // The plugin picks up the classes of the `java` plugin's main source set, which a multiplatform project does not have
    classes.from(kotlin.jvm().compilations.named("main").map { it.output.classesDirs })
    outputFile = file("api/language-syntax-api.foreign")
}
