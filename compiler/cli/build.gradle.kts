plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    implementation(project(":analysis:light-classes-base"))
    implementation(project(":compiler:util"))
    api(project(":compiler:cli-base"))
    implementation(project(":compiler:javac-wrapper"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:backend-common"))
    implementation(project(":compiler:backend"))
    implementation(project(":compiler:backend.jvm.entrypoint"))
    implementation(project(":compiler:cli-common"))
    implementation(project(":compiler:serialization"))
    implementation(project(":compiler:plugin-api"))
    implementation(commonDependency("org.fusesource.jansi", "jansi"))
    implementation(project(":compiler:fir:raw-fir:psi2fir"))
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:providers"))
    implementation(project(":compiler:fir:semantics"))
    implementation(project(":compiler:fir:java"))
    implementation(project(":compiler:fir:entrypoint"))
    implementation(project(":compiler:fir:fir2ir"))
    implementation(project(":compiler:fir:fir2ir:jvm-backend"))
    implementation(project(":compiler:fir:checkers"))
    implementation(project(":compiler:fir:checkers:checkers.jvm"))
    implementation(project(":compiler:fir:checkers:checkers.js"))
    implementation(project(":compiler:fir:checkers:checkers.native"))
    implementation(project(":compiler:fir:checkers:checkers.wasm"))
    implementation(project(":compiler:fir:fir-serialization"))
    implementation(project(":compiler:ir.inline"))
    implementation(project(":compiler:ir.actualization"))
    implementation(project(":kotlin-util-io"))
    implementation(project(":js:js.config"))
    implementation(project(":wasm:wasm.config"))
    implementation(project(":wasm:wasm.frontend"))

    compileOnly(toolsJarApi())
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDirs("../builtins-serializer/src")
    }
}

allprojects {
    optInToExperimentalCompilerApi()
}
