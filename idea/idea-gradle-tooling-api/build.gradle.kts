plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(intellijPluginDep("gradle"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}