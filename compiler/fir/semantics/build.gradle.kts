plugins {
    kotlin("jvm")
    id("power-assert-convention")
}

dependencies {
    api(project(":compiler:fir:providers"))
    implementation(project(":core:util.runtime"))

    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
