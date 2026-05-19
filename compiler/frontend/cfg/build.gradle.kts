plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:resolution"))
    implementation(project(":core:descriptors"))
    compileOnly(intellijCore())
    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
