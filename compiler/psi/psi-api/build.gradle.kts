plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":compiler:util"))
    api(project(":compiler:frontend.common"))
    api(project(":kotlin-script-runtime"))

    compileOnly(intellijCore())
    compileOnly(libs.guava)
    compileOnly(libs.intellij.fastutil)

    testFixturesApi(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testFixturesImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testFixturesCompileOnly(intellijCore())
    testCompileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true

        filters {
            excluded.annotatedWith.add("org.jetbrains.kotlin.psi.KtImplementationDetail")
        }
    }
}
tasks.check {
    dependsOn(tasks.checkLegacyAbi)
}

testsJar()

projectTest(jUnitMode = JUnitMode.JUnit5) {
    workingDir = rootDir
    useJUnitPlatform()
}
