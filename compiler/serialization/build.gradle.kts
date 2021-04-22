plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:resolution"))
    compile(project(":core:deserialization"))
    api(project(":compiler:util"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core", rootProject = rootProject) }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
