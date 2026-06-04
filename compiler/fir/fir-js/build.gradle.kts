plugins {
    kotlin("jvm")
    id("power-assert-convention")
}

dependencies {
    implementation(project(":js:js.frontend.common"))
    api(project(":compiler:fir:resolve"))
}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}
