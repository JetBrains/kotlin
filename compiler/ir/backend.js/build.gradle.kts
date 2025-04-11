plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:cli-base"))
    api(project(":compiler:util"))
    api(project(":compiler:frontend"))
    api(project(":compiler:backend-common"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:ir.inline"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":compiler:ir.serialization.js"))
    api(project(":js:js.ast"))
    api(project(":js:js.sourcemap"))
    implementation(project(":js:js.translator"))

    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

// This is only a temporary (~days) measure to allow deprecating old IR parameter API (KT-73189) just before we finish the migration (KT-73226).
optInTo("org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi")

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
