plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:build-tools:kotlin-build-tools-api"))
    implementation(kotlinStdlib())
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:cli-js"))
    compileOnly(project(":kotlin-build-common"))
    compileOnly(project(":daemon-common"))
    compileOnly(project(":kotlin-daemon-client"))
    compileOnly(project(":compiler:incremental-compilation-impl"))
    compileOnly(project(":kotlin-compiler-runner-unshaded"))
    compileOnly(intellijCore())
    compileOnly(project(":kotlin-scripting-compiler"))
    runtimeOnly(project(":kotlin-compiler-embeddable"))
    runtimeOnly(project(":kotlin-compiler-runner"))
    runtimeOnly(project(":kotlin-scripting-compiler-embeddable"))
    runtimeOnly(project(":kotlin-scripting-compiler-impl-embeddable"))
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler {
    from(mainSourceSet.output)
})
sourcesJar()
javadocJar()

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
    }
}