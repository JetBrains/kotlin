plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(kotlinStdlib())
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

val generateGradleByIml by generator("org.jetbrains.kotlin.generators.imltogradle.MainKt")
