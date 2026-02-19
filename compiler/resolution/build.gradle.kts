plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":core:descriptors"))
    api(project(":compiler:resolution.common"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.fastutil)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
