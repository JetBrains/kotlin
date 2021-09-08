plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:config"))
    api(project(":core:compiler.common.jvm"))
    compileOnly(intellijCoreDep()) { includeJars("asm-all", rootProject = rootProject) }
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}
