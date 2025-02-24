plugins {
    kotlin("jvm")
    id("jps-compatible")
    application
}

dependencies {
    implementation(project(":generators"))
    implementation(project(":generators:tree-generator-common"))
    implementation(project(":compiler:fir:tree:tree-generator"))
    implementation(project(":compiler:config"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":js:js.frontend"))
    implementation(project(":kotlin-util-klib"))
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    /*
     We do not need guava in the generator, but because of a bug in the IJ project importing, we need to have a dependency on intellijCore
     the same as it is in `:fir:tree:tree-generator` module for the project to be imported correctly
    */
    compileOnly(intellijCore())
    compileOnly(libs.guava)
}

application {
    mainClass.set("org.jetbrains.kotlin.config.keys.generator.MainKt")
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
