import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(project(":core:compiler.common"))
    implementation(project(":analysis:analysis-api"))
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":core:deserialization.common.jvm"))
    implementation(project(":core:language.targets.jvm"))
    implementation(project(":core:language.version-settings"))
    implementation(project(":compiler:util"))
    implementation(intellijCore())
    implementation(libs.opentelemetry.api)
    implementation(libs.caffeine)

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testRuntimeOnly(libs.junit.jupiter.engine)
}

kotlin {
    explicitApi()

    compilerOptions {
        optIn.add("org.jetbrains.kotlin.analysis.api.KaPlatformInterface")
    }

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        referenceDumpDir = File("api-unstable")

        filters {
            exclude.annotatedWith.addAll(
                "org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
            )
        }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

projectTests {
    testCodebaseTask(dumpDirs = listOf("api", "api-unstable"))
}
