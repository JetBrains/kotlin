plugins {
    id("root-config")
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:frontend.java"))

    compileOnly(toolsJarApi())
    compileOnly(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" { }
}
