plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":js:js.frontend.common"))
    api(project(":compiler:fir:resolve"))
}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}
