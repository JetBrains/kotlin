import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
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
    implementation(project(":compiler:psi"))
    implementation(kotlinxCollectionsImmutable())

    compileOnly(intellijCore())
    compileOnly(libs.guava)

    testImplementation(libs.junit4)
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:fir:raw-fir:psi2fir"))

    testCompileOnly(kotlinTest("junit"))

    testRuntimeOnly(project(":core:descriptors.runtime"))

    testCompileOnly(intellijCore())
    testRuntimeOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTest {
    workingDir = rootDir
}

testsJar()
