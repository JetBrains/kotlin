plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:bir.tree"))

    api(project(":compiler:backend-common"))
    api(project(":compiler:backend.jvm"))
    api(project(":compiler:util"))
    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.compileKotlin {
    compilerOptions {
        allWarningsAsErrors.set(false)
    }
}
