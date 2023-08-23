plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:cli-base"))
    api(project(":compiler:frontend"))
    api(project(":compiler:backend-common"))
    api(project(":compiler:backend"))
    implementation(project(":compiler:backend.jvm.entrypoint"))
    api(project(":compiler:serialization"))
    api(project(":compiler:plugin-api"))
    api(project(":js:js.translator"))
    api(commonDependency("org.fusesource.jansi", "jansi"))
    api(project(":compiler:fir:raw-fir:psi2fir"))
    api(project(":compiler:fir:resolve"))
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:semantics"))
    api(project(":compiler:fir:java"))
    api(project(":compiler:fir:entrypoint"))
    api(project(":compiler:fir:fir2ir"))
    implementation(project(":compiler:fir:fir2ir:jvm-backend"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:checkers:checkers.jvm"))
    api(project(":compiler:fir:checkers:checkers.js"))
    api(project(":compiler:fir:checkers:checkers.native"))
    api(project(":compiler:fir:fir-serialization"))
    api(project(":kotlin-util-io"))

    compileOnly(toolsJarApi())
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    testApi(project(":compiler:backend"))
    testApi(project(":compiler:cli"))
    testApi(projectTests(":compiler:tests-common"))
    testImplementation(libs.junit4)
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDirs("../builtins-serializer/src")
    }
    "test" { }
}

allprojects {
    optInToExperimentalCompilerApi()
}

testsJar {}

projectTest {
    workingDir = rootDir
}
