plugins {
    kotlin("jvm")
    id("require-explicit-types")
}

dependencies {
    implementation(project(":js:js.frontend.common"))
    api(project(":compiler:fir:resolve"))
}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}
