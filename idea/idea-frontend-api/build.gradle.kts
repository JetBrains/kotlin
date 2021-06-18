plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":kotlin-reflect-api"))

    compileOnly(project(":compiler:psi"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":core:compiler.common"))
    compileOnly(project(":core:compiler.common.jvm"))
    compileOnly(project(":idea:idea-frontend-independent"))
    compileOnly(project(":idea:idea-frontend-fir:idea-fir-low-level-api"))
    compileOnly(project(":idea:idea-frontend-independent"))
    compileOnly(intellijCoreDep())
    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}
