plugins {
    kotlin("jvm")
    id("jps-compatible")
}

kotlin {
    explicitApiWarning()
}

dependencies {
    compileOnly(project(":kotlin-reflect-api"))

    compileOnly(project(":compiler:psi"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":core:compiler.common"))
    compileOnly(project(":core:compiler.common.jvm"))
    compileOnly(project(":idea-frontend-fir:idea-fir-low-level-api"))
    implementation(project(":analysis:analysis-api-providers"))

    compile(intellijCoreDep()) { includeJars("intellij-core", "guava", rootProject = rootProject) }
}

kotlin {
    explicitApi()
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
