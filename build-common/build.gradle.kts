description = "Kotlin Build Common"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":compiler:backend.common.jvm"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":js:js.serializer"))
    compileOnly(project(":js:js.config"))
    compileOnly(project(":kotlin-util-klib-metadata"))
    compileOnly(project(":kotlin-reflect-api"))

    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
    compileOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))

    testCompileOnly(project(":compiler:cli-common"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(commonDependency("junit:junit"))
    testApi(protobufFull())
    testApi(kotlinStdlib())
    testImplementation(project(":kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()

testsJar()

projectTest(parallel = true)
