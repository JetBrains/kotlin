plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:cli-common"))
    api(project(":compiler:frontend"))
    api(project(":compiler:frontend.java"))
    api(project(":compiler:frontend:cfg"))
    api(project(":compiler:backend-common"))
    api(project(":compiler:backend"))
    api(project(":compiler:backend.jvm"))
    implementation(project(":compiler:backend.jvm.entrypoint"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:light-classes"))
    api(project(":compiler:serialization"))
    api(project(":compiler:plugin-api"))
    api(project(":compiler:javac-wrapper"))
    api(project(":js:js.translator"))
    api(project(":native:frontend.native"))
    api(commonDependency("org.fusesource.jansi", "jansi"))
    api(commonDependency("org.jline", "jline"))
    api(project(":compiler:fir:raw-fir:psi2fir"))
    api(project(":compiler:fir:resolve"))
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:semantics"))
    api(project(":compiler:fir:java"))
    implementation(project(":compiler:fir:entrypoint"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:fir:fir2ir:jvm-backend"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:checkers:checkers.jvm"))
    api(project(":compiler:fir:checkers:checkers.js"))
    api(project(":kotlin-util-klib"))
    api(project(":kotlin-util-io"))

    // TODO: as soon as cli-jvm is extracted out of this module, move this dependency there
    compileOnly(project(":compiler:ir.tree.impl"))

    compileOnly(toolsJarApi())
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))

    testApi(project(":compiler:backend"))
    testApi(project(":compiler:cli"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(commonDependency("junit:junit"))
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDirs("../builtins-serializer/src")
    }
    "test" { }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        languageVersion = "1.4"
        apiVersion = "1.4"
        freeCompilerArgs = freeCompilerArgs - "-progressive" + listOf(
            "-Xskip-prerelease-check", "-Xsuppress-version-warnings", "-Xuse-mixed-named-arguments", "-Xnew-inference"
        )
    }
}

testsJar {}

projectTest {
    workingDir = rootDir
}
