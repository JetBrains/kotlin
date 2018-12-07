plugins {
    kotlin("jvm")
}

apply { plugin("jps-compatible") }

dependencies {
    compile(project(":compiler:util"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("gradle"))
    compileOnly(intellijPluginDep("android"))
}

sourceSets {
    if (Ide.IJ()) {
        "main" {
            projectDefault()
        }
    } else {
        "main" {}
    }
    "test" {}
}

runtimeJar {
    archiveName = "android-output-parser-ide.jar"
}

ideaPlugin()
