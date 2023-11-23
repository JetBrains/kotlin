plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:fir:checkers"))
    implementation(project(":core:compiler.common.web"))

    /*
     * We can't remove this dependency until we use
     *   diagnostics framework from FE 1.0
     */
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:psi"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
        generatedDir()
    }
    "test" { none() }
}
