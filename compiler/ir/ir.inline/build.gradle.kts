plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:ir.backend.native"))

    implementation(project(":core:descriptors"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":core:error.handling"))
    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
