plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:frontend"))
    api(project(":compiler:config.web"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
}