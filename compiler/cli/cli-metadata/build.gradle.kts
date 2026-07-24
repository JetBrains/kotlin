plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    implementation(project(":compiler:util"))
    implementation(project(":compiler:cli"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:config.jvm"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":core:deserialization"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:backend"))
    implementation(project(":compiler:fir:semantics"))
    implementation(project(":compiler:fir:fir-jvm"))
    implementation(project(":compiler:fir:fir2ir"))
    implementation(project(":compiler:fir:fir-serialization"))
    implementation(project(":compiler:serialization"))
    implementation(project(":wasm:wasm.frontend"))
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:providers"))
    implementation(project(":compiler:fir:entrypoint"))
    implementation(project(":kotlin-util-io"))
    implementation(project(":js:js.config"))
    implementation(project(":native:native.config"))
    implementation(project(":kotlin-util-klib-metadata"))

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
