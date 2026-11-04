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
    implementation(project(":compiler:fir:tree"))
    implementation(project(":compiler:fir:fir2ir"))
    implementation(project(":compiler:fir:diagnostic-renderers"))
    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
