import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"
javaHome = rootProject.extra["JDK_16"] as String

dependencies {
    compile(protobufLite())
    compile(project(":kotlin-stdlib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.withType<JavaCompile> {
    dependsOn(protobufLiteTask)
    sourceCompatibility = "1.6"
    targetCompatibility = "1.6"
}

tasks.withType<KotlinCompile> {
    dependsOn(protobufLiteTask)
}
