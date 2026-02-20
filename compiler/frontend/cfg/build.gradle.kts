plugins {
    id("root-config")
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:frontend"))
    compileOnly(intellijCore())
    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
