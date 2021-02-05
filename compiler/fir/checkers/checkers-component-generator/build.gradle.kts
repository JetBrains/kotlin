plugins {
    kotlin("jvm")
    id("jps-compatible")
    application
}

dependencies {
    implementation(project(":compiler:fir:tree"))
    implementation(project(":compiler:fir:tree:tree-generator"))
    implementation(project(":kotlin-reflect"))
    implementation(project(":kotlin-reflect-api"))

    /*
     We do not need guava in the generator, but because of a bug in the IJ project importing, we need to have a dependency on intellijCoreDep
     the same as it is in `:fir:tree:tree-generator` module to the project be imported correctly
    */
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "guava", rootProject = rootProject) }

    implementation(project(":compiler:psi"))
}

val writeCopyright by task<tasks.WriteCopyrightToFile> {
    outputFile.set(file("$buildDir/copyright/notice.txt"))
    commented.set(true)
}

application {
    mainClassName = "org.jetbrains.kotlin.fir.checkers.generator.MainKt"
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
