plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":js:js.config"))
    api(project(":compiler:fir:resolve"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
