plugins {
    kotlin("jvm")
    id("jps-compatible")
    application
}

dependencies {
    implementation(project(":compiler:resolution.common"))
    implementation(project(":compiler:fir:tree"))
    implementation(project(":compiler:fir:tree:tree-generator"))
    implementation(project(":compiler:fir:checkers:checkers-component-generator"))
    implementation(project(":analysis:analysis-api"))

    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    /*
     We do not need guava in the generator, but because of a bug in the IJ project importing, we need to have a dependency on intellijCore
     the same as it is in `:fir:tree:tree-generator` module to the project be imported correctly
     */
    compileOnly(intellijCore())
    compileOnly(commonDependency("com.google.guava:guava"))

    implementation(project(":compiler:psi"))
}

application {
    mainClass.set("org.jetbrains.kotlin.analysis.api.fir.generator.MainKt")
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
