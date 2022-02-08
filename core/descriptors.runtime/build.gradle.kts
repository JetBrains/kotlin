import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

tasks
    .matching { it.name == "compileKotlin" && it is KotlinCompile }
    .configureEach {
        (this as KotlinCompile).configureTaskToolchain(JdkMajorVersion.JDK_1_6)
    }

tasks
    .matching { it.name == "compileJava" && it is JavaCompile }
    .configureEach {
        (this as JavaCompile).configureTaskToolchain(JdkMajorVersion.JDK_1_6)
    }

dependencies {
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":core:descriptors"))
    compileOnly(project(":core:descriptors.jvm"))

    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":generators:test-generator"))

    testApi(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateRuntimeDescriptorTestsKt")

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
}

testsJar()
