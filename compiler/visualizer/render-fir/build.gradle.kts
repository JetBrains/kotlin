plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":compiler:fir:psi2fir"))
    compile(project(":compiler:fir:resolve"))
    compile(project(":compiler:visualizer:common"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}
