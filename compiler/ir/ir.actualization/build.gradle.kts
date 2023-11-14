plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":compiler:util"))
    api(project(":compiler:ir.tree"))
    api(project(":core:compiler.common"))
    implementation(project(":compiler:resolution.common"))
    implementation(project(":compiler:frontend"))
    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

