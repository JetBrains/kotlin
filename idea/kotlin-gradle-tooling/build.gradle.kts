
description = "Kotlin Gradle Tooling support"

apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":compiler:cli-common"))
    compile(intellijPluginDep("gradle")) {
        includeJars("gradle-api",
                    "gradle-tooling-extension-api",
                    "gradle",
                    rootProject = rootProject)
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()

ideaPlugin()
