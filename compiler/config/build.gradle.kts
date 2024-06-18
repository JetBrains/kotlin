plugins {
    kotlin("jvm")
    id("jps-compatible")
//    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    api(project(":core:metadata"))
    api(project(":core:compiler.common"))
    api(project(":compiler:util"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
