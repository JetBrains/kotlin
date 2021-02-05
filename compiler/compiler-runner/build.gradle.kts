description = "Compiler runner + daemon client"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":kotlin-build-common"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":kotlin-preloader"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":daemon-common"))
    compileOnly(project(":daemon-common-new"))
    compile(projectRuntimeJar(":kotlin-daemon-client"))
    compileOnly(project(":compiler:util"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    runtimeOnly(projectRuntimeJar(":kotlin-compiler-embeddable"))
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJar()
javadocJar()
