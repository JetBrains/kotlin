
plugins {
    java
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"
javaHome = rootProject.extra["JDK_16"] as String

dependencies {
    compileOnly(project(":kotlin-stdlib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.6"
    targetCompatibility = "1.6"
}

if (project.hasProperty("teamcity"))
tasks["compileJava"].dependsOn(":prepare:build.version:writeCompilerVersion")