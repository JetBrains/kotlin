import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":core:descriptors"))
    compileOnly(project(":core:descriptors.jvm"))

    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":generators:test-generator"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val compileJava by tasks.getting(JavaCompile::class) {
    sourceCompatibility = "1.6"
    targetCompatibility = "1.6"
}

val compileKotlin by tasks.getting(KotlinCompile::class) {
    kotlinOptions.jvmTarget = "1.6"
    kotlinOptions.jdkHome = rootProject.extra["JDK_16"] as String
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateRuntimeDescriptorTestsKt")

projectTest {
    workingDir = rootDir
}
