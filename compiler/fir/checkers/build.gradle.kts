plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("generators.checkers.generated-sources")
}

dependencies {
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:semantics"))
    api(project(":compiler:fir:raw-fir:raw-fir.common"))
    implementation(project(":compiler:frontend.common"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:psi"))

    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" { none() }
}
