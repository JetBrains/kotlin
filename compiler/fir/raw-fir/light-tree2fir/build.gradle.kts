plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("require-explicit-types")
}

group = "org.jetbrains.kotlin.fir"

dependencies {
    api(project(":compiler:fir:raw-fir:raw-fir.common"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:psi:psi-impl"))
    implementation(project(":compiler:psi:psi-frontend-utils"))
    implementation(project(":compiler:psi:parser"))
    implementation(kotlinxCollectionsImmutable())

    compileOnly(intellijCore())
    compileOnly(libs.guava)

    testFixturesApi(libs.junit4)
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:fir:raw-fir:psi2fir")))
    testFixturesApi(testFixtures(project(":compiler:fir:raw-fir:raw-fir.common")))

    testCompileOnly(kotlinTest("junit"))

    testFixturesCompileOnly(intellijCore())
    testImplementation(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit4) {
        workingDir = rootDir
    }

    testGenerator("org.jetbrains.kotlin.fir.lightTree.TestGeneratorForLightTree2FirKt")
}

testsJar()
