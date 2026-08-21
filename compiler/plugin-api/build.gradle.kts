plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:config"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

optInToExperimentalCompilerApi()
