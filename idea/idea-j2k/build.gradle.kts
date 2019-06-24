plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":j2k"))
    compile(project(":idea:idea-core"))

    compileOnly(intellijDep())
    
    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}