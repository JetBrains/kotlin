plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:compiler.common.jvm"))
    api(project(":compiler:config.jvm"))
    api(intellijCoreDep()) { includeJars("asm-all", "guava", rootProject = rootProject) }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
