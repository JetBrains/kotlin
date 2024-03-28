plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:bir.backend"))

    //api(project(":compiler:backend"))
    //api(project(":compiler:backend.common.jvm"))
    //api(project(":compiler:ir.tree"))
    //api(project(":compiler:ir.backend.common"))
    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}

tasks.compileKotlin {
    compilerOptions {
        allWarningsAsErrors.set(false)
    }
}
