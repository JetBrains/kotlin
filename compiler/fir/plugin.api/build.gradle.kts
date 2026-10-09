plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("require-explicit-types")
}

kotlin {
    explicitApiWarning()
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":compiler:fir:semantics.api"))
    implementation(project(":core:util.runtime"))

    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
