plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(kotlinStdlib())
    api(project(":compiler:compiler.version"))
    compile(project(":core:deserialization"))
    compileOnly(project(":kotlin-reflect-api"))

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
