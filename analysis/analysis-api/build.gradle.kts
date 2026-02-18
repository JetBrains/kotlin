import org.jetbrains.kotlin.build.foreign.CheckForeignClassUsageTask
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    kotlin("jvm")
    id("kotlin-git.gradle-build-conventions.foreign-class-usage-checker")
    id("project-tests-convention")
    id("test-inputs-check")
}

kotlin {
    explicitApiWarning()
}

dependencies {
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    compileOnly(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:backend"))
    compileOnly(project(":core:compiler.common"))
    compileOnly(project(":core:compiler.common.jvm"))
    compileOnly(project(":core:compiler.common.js"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:kt-references"))

    api(intellijCore())
    api(libs.intellij.asm)
    api(libs.guava)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(testFixtures(project(":compiler:psi:psi-api")))
    testImplementation(testFixtures(project(":compiler:tests-common")))
}

private val stableNonPublicMarkers = listOf(
    "org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
    "org.jetbrains.kotlin.analysis.api.KaNonPublicApi",
    "org.jetbrains.kotlin.analysis.api.KaIdeApi",
    "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
    "org.jetbrains.kotlin.analysis.api.KaPlatformInterface", // Platform interface is not stable yet
    "org.jetbrains.kotlin.analysis.api.KaContextParameterApi",
)

kotlin {
    explicitApi()

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)

        filters {
            exclude.annotatedWith.addAll(stableNonPublicMarkers)
        }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5)

    testData(project.isolated, "src")

    /** The 'test' task inputs cannot depend on [checkForeignClassUsage] outputs. */
    testData(project.isolated, "api/analysis-api.api")
    testData(project.isolated, "api/analysis-api.undocumented")
}

val checkForeignClassUsage by tasks.registering(CheckForeignClassUsageTask::class) {
    outputFile = file("api/analysis-api.foreign")
    nonPublicMarkers.addAll(stableNonPublicMarkers)
}

tasks.named("checkKotlinAbi").configure {
    /**
     * The ABI task depends on the whole 'api/' directory which contains output of the [checkForeignClassUsage] task.
     * Gradle requires having an explicit task dependency when inputs of a certain task contain outputs of another.
     */
    dependsOn(checkForeignClassUsage)
}