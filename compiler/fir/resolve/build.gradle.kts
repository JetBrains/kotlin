plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:semantics"))
    implementation(project(":core:util.runtime"))

    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

optInTo("org.jetbrains.kotlin.fir.scopes.ScopeFunctionRequiresPrewarm")
