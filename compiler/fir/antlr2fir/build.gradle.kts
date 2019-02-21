import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import tasks.WriteCopyrightToFile

plugins {
    kotlin("jvm")
}

group = "org.jetbrains.kotlin.fir"

dependencies {
    compile(project(":core:descriptors"))
    compile( "org.antlr", "antlr4", "4.7.1")
    compile("org.antlr", "antlr4-runtime", "4.7.1")
    compile(kotlin("reflect"))
    compile("junit", "junit", "4.4")
}

val writeCopyright by task<WriteCopyrightToFile> {
    outputFile = file("$buildDir/copyright/notice.txt")
    commented = true
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