plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":compiler:ir.tree"))

    implementation(project(":core:compiler.common.native"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
}

optInToUnsafeDuringIrConstructionAPI()
