plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:frontend.common"))
    api(project(":compiler:util"))
    api(project(":core:language.version-settings"))
    api(project(":kotlin-stdlib"))
    implementation(project(":core:compiler.common"))
    implementation(project(":core:names"))

    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
