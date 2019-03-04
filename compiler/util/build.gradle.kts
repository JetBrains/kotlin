
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(kotlinStdlib())
    compile(project(":core:deserialization"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeIntellijCoreJarDependencies(project) }
    compileOnly(jpsStandalone()) { includeJars("jps-model") }
}

sourceSets {
    "main" {
        projectDefault()
        resources.srcDir(File(rootDir, "resources"))
    }
    "test" {}
}

