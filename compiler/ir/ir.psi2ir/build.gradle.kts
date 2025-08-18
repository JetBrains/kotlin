plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:frontend"))
    api(project(":compiler:backend-common.k1"))
    api(project(":compiler:ir.tree"))
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":compiler:ir.backend.common"))
    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()
optInToObsoleteDescriptorBasedAPI()
optInTo("org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi")

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
