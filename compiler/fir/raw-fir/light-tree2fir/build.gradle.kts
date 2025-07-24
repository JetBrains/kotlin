plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
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

projectTest(jUnitMode = JUnitMode.JUnit4) {
    workingDir = rootDir
}

testsJar()
