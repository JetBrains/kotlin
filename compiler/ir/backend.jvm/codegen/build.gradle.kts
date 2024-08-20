plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:backend.jvm"))
    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()
kotlin {
    compilerOptions.optIn.add("org.jetbrains.kotlin.ir.util.JvmIrInlineExperimental")
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
