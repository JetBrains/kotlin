plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":core:descriptors"))
    api(project(":compiler:resolution.common"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core", rootProject = rootProject) }
    compileOnly(intellijDep()) { includeJars("trove4j") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
