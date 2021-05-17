plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(kotlinStdlib("jdk8"))
    implementation(intellijDep())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val generateIdeaGradleFiles by generator("org.jetbrains.kotlin.generators.imltogradle.MainKt")
