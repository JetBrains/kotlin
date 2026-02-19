plugins {
    kotlin("jvm")
    id("project-tests-convention")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":compiler:build-tools:kotlin-build-tools-api"))
    implementation(kotlinStdlib())
    compileOnly(project(":kotlin-build-common"))
    compileOnly(project(":core:compiler.common"))

    compileOnly(libs.kotlinx.serialization.protobuf)
    embedded(libs.kotlinx.serialization.protobuf) { isTransitive = false }
    embedded(libs.kotlinx.serialization.core) { isTransitive = false }

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(project(":kotlin-build-common"))
    testImplementation(project(":core:compiler.common"))
    testImplementation(libs.kotlinx.serialization.protobuf)
}

publish()

runtimeJarWithRelocation {
    from(mainSourceSet.output)
    relocate("kotlinx.serialization", "org.jetbrains.kotlin.buildtools.internal.serialization")
}

sourcesJar()
javadocJar()

kotlin {
    explicitApi()
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
    }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5)
}
