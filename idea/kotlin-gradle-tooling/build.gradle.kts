
description = "Kotlin Gradle Tooling support"

apply { plugin("kotlin") }

dependencies {
    compile(project(":kotlin-stdlib"))
    compile(project(":compiler:cli-common"))
    compile(ideaSdkDeps("gradle-tooling-api",
                        "gradle-tooling-extension-api",
                        "gradle",
                        "gradle-core",
                        "gradle-base-services-groovy",
                        subdir = "plugins/gradle/lib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()

ideaPlugin()
