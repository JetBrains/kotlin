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
    compile(project(":compiler:psi"))
    compile(project(":compiler:frontend.common"))
    compile(project(":core:descriptors"))
    compile(project(":compiler:fir:cones"))
    compile(project(":compiler:resolution"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "guava", rootProject = rootProject) }
    Platform[193].orLower {
        compileOnly(intellijDep()) { includeJars("picocontainer", rootProject = rootProject) }
    }
    compileOnly(intellijDep()) {
        includeJars("trove4j", rootProject = rootProject)
    }

    Platform[192].orHigher {
        runtimeOnly(intellijCoreDep()) { includeJars("jdom") }
    }
    implementation(kotlin("reflect"))
}

val writeCopyright by task<WriteCopyrightToFile> {
    outputFile = file("$buildDir/copyright/notice.txt")
    commented = true
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
