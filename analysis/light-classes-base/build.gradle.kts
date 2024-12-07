plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:frontend.common"))
    api(project(":compiler:frontend.common-psi"))
    api(project(":compiler:frontend.common.jvm"))
    api(project(":compiler:resolution.common.jvm"))
    api(project(":core:compiler.common.jvm"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
}
