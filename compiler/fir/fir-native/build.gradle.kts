plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:compiler.common.native"))
    implementation(project(":compiler:frontend.common"))
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:fir2ir"))
}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}
