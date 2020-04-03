plugins {
    kotlin("jvm")
    id("jps-compatible")
}

// BUNCH 193: this module is no longer needed since IDEA 2020.1
Platform[193].orLower {
    dependencies {
        compileOnly(intellijPluginDep("gradle"))
    }

    sourceSets {
        "main" { projectDefault() }
        "test" { projectDefault() }
    }

    runtimeJar()

    sourcesJar()

    javadocJar()

    apply(from = "$rootDir/gradle/kotlinPluginPublication.gradle.kts")
}

