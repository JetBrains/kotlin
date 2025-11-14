plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
}

group = "org.jetbrains.kotlin.fir"

repositories {
    mavenCentral()
    mavenLocal()
    maven { setUrl("https://www.jetbrains.com/intellij-repository/releases") }
    maven { setUrl("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") }
}

dependencies {
    api(project(":compiler:fir:raw-fir:raw-fir.common"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:psi:psi-impl"))
    implementation(project(":compiler:psi:parser"))
    implementation(kotlinxCollectionsImmutable())

    compileOnly(intellijCore())
    compileOnly(libs.guava)

    testFixturesApi(libs.junit4)
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:fir:raw-fir:psi2fir")))

    testCompileOnly(kotlinTest("junit"))

    testRuntimeOnly(project(":core:descriptors.runtime"))

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
