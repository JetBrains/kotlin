import tasks.WriteCopyrightToFile

plugins {
    kotlin("jvm")
    id("jps-compatible")
    application
}

val runtimeOnly by configurations
val compileOnly by configurations
runtimeOnly.extendsFrom(compileOnly)

dependencies {
    implementation(project(":generators"))
    implementation(project(":core:compiler.common"))
    implementation(project(":compiler:frontend.common"))
    implementation(project(":compiler:fir:cones"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "guava", rootProject = rootProject) }
    Platform[193].orLower {
        compileOnly(intellijDep()) { includeJars("picocontainer", rootProject = rootProject) }
    }
    compileOnly(intellijDep()) {
        includeJars("trove4j", rootProject = rootProject)
    }

    runtimeOnly(intellijCoreDep()) { includeJars("jdom") }
}

val writeCopyright by task<WriteCopyrightToFile> {
    outputFile.set(file("$buildDir/copyright/notice.txt"))
    commented.set(true)
}

application {
    mainClassName = "org.jetbrains.kotlin.fir.tree.generator.MainKt"
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
