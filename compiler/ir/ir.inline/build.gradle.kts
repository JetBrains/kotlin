plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:ir.backend.native"))

    implementation(project(":compiler:frontend.common-psi"))
    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

