plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":compiler:ir.tree"))

    implementation(project(":compiler:resolution"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":core:descriptors"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
}

optInToUnsafeDuringIrConstructionAPI()
