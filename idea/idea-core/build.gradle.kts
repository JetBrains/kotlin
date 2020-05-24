
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(kotlinStdlib())
    compileOnly(project(":kotlin-reflect-api"))
    compile(project(":compiler:psi"))
    compile(project(":core:descriptors"))
    compile(project(":core:descriptors.jvm"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:util"))
    compile(project(":idea:ide-common"))
    compile(project(":idea:idea-jps-common"))
    compile(project(":kotlin-util-klib-metadata"))
    compile(project(":plugins:android-extensions-compiler"))
    compile(project(":kotlin-scripting-compiler-impl-unshaded"))
    compile(project(":compiler:fir:raw-fir:psi2fir"))
    compile(project(":compiler:fir:fir2ir"))
    compile(project(":compiler:fir:resolve"))
    compile(project(":compiler:fir:checkers"))
    compile(project(":compiler:fir:java"))
    compile(project(":compiler:fir:jvm"))
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")) { isTransitive = false }

    compileOnly(intellijDep())

    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java"))
    }
    
    compileOnly(intellijPluginDep("gradle"))
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir("../idea-analysis/src")
        resources.srcDir("../idea-analysis/resources")
    }
    "test" {}
}
