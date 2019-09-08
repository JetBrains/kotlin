plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":compiler:visualizer:common"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}
