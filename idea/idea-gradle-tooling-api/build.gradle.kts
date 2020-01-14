plugins {
    kotlin("jvm")
    id("jps-compatible")
}

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
