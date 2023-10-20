plugins {
    application
    kotlin("jvm")
    id("jps-compatible")
    kotlin("plugin.serialization")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }

    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":compiler:fir:analysis-tests"))
    testImplementation(projectTests(":generators:test-generator"))

    testImplementation(project(":compiler:fir:checkers"))
    testImplementation(project(":compiler:fir:checkers:checkers.jvm"))
    testImplementation(project(":compiler:fir:checkers:checkers.js"))
    testImplementation(project(":compiler:fir:checkers:checkers.native"))
    testImplementation(project(":plugins:android-extensions-compiler"))
    testImplementation(project(":plugins:fir-plugin-prototype"))
    testImplementation(project(":plugins:parcelize:parcelize-compiler:parcelize.k1"))
    testImplementation(project(":plugins:parcelize:parcelize-compiler:parcelize.k2"))
    testImplementation(project(":kotlinx-serialization-compiler-plugin.k1"))
    testImplementation(project(":kotlinx-serialization-compiler-plugin.k2"))
    testImplementation(project(":kotlin-noarg-compiler-plugin.k1"))
    testImplementation(project(":kotlin-noarg-compiler-plugin.k2"))
    testImplementation(project(":kotlin-assignment-compiler-plugin.k1"))
    testImplementation(project(":kotlin-assignment-compiler-plugin.k2"))

    testImplementation(project(":compiler:fir:raw-fir:raw-fir.common"))
    testImplementation(project(":compiler:frontend"))
    testImplementation(project(":compiler:frontend.java"))
    testImplementation(project(":js:js.frontend"))
    testImplementation(project(":native:frontend.native"))
    testImplementation(project(":wasm:wasm.frontend"))

    // Errors.java have some ImmutableSet fields which we
    // don't need here, but otherwise getDeclaredFields fails
    testRuntimeOnly(commonDependency("com.google.guava:guava:12.0"))

    testImplementation(libs.junit4)
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    compileOnly(kotlinStdlib())
    compileOnly(intellijCore())
}

application {
    mainClass.set("org.jetbrains.kotlin.k1k2uicomparator.MainKt")
}

tasks.register<JavaExec>("runTest") {
    workingDir = rootDir
    group = ApplicationPlugin.APPLICATION_GROUP
    classpath(sourceSets.test.get().runtimeClasspath)
    mainClass.set("org.jetbrains.kotlin.k1k2uicomparator.test.RunKt")
}

sourceSets {
    "main" {
        projectDefault()
        generatedTestDir()
    }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTest(jUnitMode = JUnitMode.JUnit5, parallel = true) {
    workingDir = rootDir
    useJUnitPlatform()
}

kotlin {
    // Otherwise sometimes there will be:
    // `Internal Server Error: Please provide a valid jdkVersion`
    jvmToolchain(17)

    javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}
