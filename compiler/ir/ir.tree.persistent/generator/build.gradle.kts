plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val generatePir by generator("org.jetbrains.kotlin.ir.persistentIrGenerator.MainKt", mainSourceSet)