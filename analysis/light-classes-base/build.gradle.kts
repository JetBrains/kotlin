plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:util"))
    implementation(project(":compiler:frontend.common"))
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(project(":core:compiler.common.jvm"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
}
