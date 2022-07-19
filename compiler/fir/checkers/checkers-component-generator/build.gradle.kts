plugins {
    kotlin("jvm")
    id("jps-compatible")
    application
}

dependencies {
    implementation(project(":generators"))
    implementation(project(":compiler:fir:tree"))
    implementation(project(":compiler:fir:tree:tree-generator"))
    implementation(project(":compiler:resolution.common"))
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    /*
     We do not need guava in the generator, but because of a bug in the IJ project importing, we need to have a dependency on intellijCore
     the same as it is in `:fir:tree:tree-generator` module to the project be imported correctly
    */
    compileOnly(intellijCore())
    compileOnly(commonDependency("com.google.guava:guava"))

    implementation(project(":compiler:psi"))
}

val writeCopyright by task<tasks.WriteCopyrightToFile> {
    outputFile.set(file("$buildDir/copyright/notice.txt"))
    commented.set(true)
}

application {
    mainClass.set("org.jetbrains.kotlin.fir.checkers.generator.MainKt")
}

val processResources by tasks
processResources.dependsOn(writeCopyright)

sourceSets {
    "main" {
        projectDefault()
        resources.srcDir("$buildDir/copyright")
    }
    "test" {}
}
