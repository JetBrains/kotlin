
description = "Kotlin Gradle Tooling support"

apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":compiler:cli-common"))
    compile(intellijPluginDep("gradle")) {
        includeJars("gradle-tooling-api-3.5",
                    "gradle-tooling-extension-api",
                    "gradle",
                    "gradle-core-3.5",
                    "gradle-base-services-groovy-3.5")
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()

ideaPlugin()
