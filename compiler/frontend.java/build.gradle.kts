plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":core:descriptors"))
    compile(project(":core:descriptors.jvm"))
    compile(project(":compiler:util"))
    compile(project(":compiler:config.jvm"))
    compile("javax.annotation:jsr250-api:1.0")
    compile(project(":compiler:frontend"))
    api(project(":compiler:resolution.common.jvm"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("asm-all", "trove4j", "guava", rootProject = rootProject) }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

