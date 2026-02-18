plugins {
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
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
