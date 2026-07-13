plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:cli-base"))
    implementation(project(":compiler:container"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":compiler:frontend:cfg"))
    implementation(project(":compiler:config.jvm"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:backend"))
    api(commonDependency("org.fusesource.jansi", "jansi"))
    api(project(":compiler:fir:resolve"))
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:semantics"))
    api(project(":compiler:fir:fir-jvm"))
    api(project(":compiler:fir:entrypoint"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:fir:checkers"))
    api(project(":kotlin-util-io"))
    implementation(project(":js:js.config"))
    implementation(project(":native:native.config"))
    implementation(project(":wasm:wasm.config"))
    implementation(project(":wasm:wasm.frontend"))

    compileOnly(toolsJarApi())
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(commonDependency("org.jetbrains.intellij.deps:jdom:2.0.6"))
    compileOnly(libs.kotlinx.coroutines.core.jvm)
}

sourceSets {
    "main" { projectDefault() }
}

optInToExperimentalCompilerApi()
