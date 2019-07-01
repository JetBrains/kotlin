plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":idea"))

    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("git4idea"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {  }
}
