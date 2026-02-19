plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:backend.jvm"))
    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
