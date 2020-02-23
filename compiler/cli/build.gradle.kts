plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.8"

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:backend-common"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:backend.jvm"))
    compile(project(":compiler:ir.backend.common"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:serialization"))
    compile(project(":compiler:plugin-api"))
    compile(project(":js:js.translator"))
    compile(project(":native:frontend.native"))
    compile(commonDep("org.fusesource.jansi", "jansi"))
    compile(commonDep("org.jline", "jline"))
    compile(project(":compiler:fir:psi2fir"))
    compile(project(":compiler:fir:resolve"))
    compile(project(":compiler:fir:jvm"))
    compile(project(":compiler:fir:java"))
    compile(project(":compiler:fir:fir2ir"))
    compile(project(":kotlin-util-klib"))
    compile(project(":kotlin-util-io"))
    compile(project(":compiler:ir.serialization.common"))

    compileOnly(toolsJarApi())
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeIntellijCoreJarDependencies(project) }

    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:cli"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDirs("../builtins-serializer/src",
                     "../javac-wrapper/src")
    }
    "test" { }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        languageVersion = "1.2"
        apiVersion = "1.2"
        freeCompilerArgs += "-Xskip-metadata-version-check"
    }
}

testsJar {}

projectTest {
    workingDir = rootDir
}
