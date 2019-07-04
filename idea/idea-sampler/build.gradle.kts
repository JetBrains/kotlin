plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":idea"))
    compileOnly(project(":idea:idea-core"))

    compileOnly(intellijDep())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
