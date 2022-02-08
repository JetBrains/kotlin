description = "Compiler runner + daemon client"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    embedded(project(":kotlin-compiler-runner-unshaded")) { isTransitive = false }

    api(project(":kotlin-build-common"))
    api(project(":kotlin-daemon-client"))
    api(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }

    runtimeOnly(project(":kotlin-compiler-embeddable"))
}

sourceSets {
    "main" { }
    "test" { }
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJar()
javadocJar()
