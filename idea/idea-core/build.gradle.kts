
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compileOnly(project(":kotlin-reflect-api"))
    compile(project(":core:descriptors"))
    compile(project(":core:descriptors.jvm"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:frontend.script"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:util"))
    compile(project(":j2k"))
    compile(project(":idea:ide-common"))
    compile(project(":idea:idea-jps-common"))
    compile(project(":plugins:android-extensions-compiler"))
    compile(project(":kotlin-scripting-compiler"))
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")) { isTransitive = false }
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("gradle"))
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir("../idea-analysis/src")
        resources.srcDir("../idea-analysis/src").apply { include("**/*.properties") }
    }
    "test" {}
}
