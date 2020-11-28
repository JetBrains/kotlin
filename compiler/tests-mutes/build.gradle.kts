plugins {
    kotlin("jvm")
    id("jps-compatible")
    application
}

dependencies {
    api(kotlinStdlib())
    implementation("khttp:khttp:1.0.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.0")
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileKotlin.kotlinOptions.freeCompilerArgs += "-Xskip-runtime-version-check"

val mutesPackageName = "org.jetbrains.kotlin.test.mutes"

application {
    mainClassName = "$mutesPackageName.MutedTestsSyncKt"
    applicationDefaultJvmArgs = rootProject.properties.filterKeys { it.startsWith(mutesPackageName) }.map { (k, v) -> "-D$k=$v" }
}
