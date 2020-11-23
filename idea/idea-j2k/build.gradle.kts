plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":j2k"))
    compile(project(":idea:idea-core"))

    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("java"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}