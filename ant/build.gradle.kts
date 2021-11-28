
description = "Kotlin Ant Tools"

plugins {
    kotlin("jvm")
}

dependencies {
    api(commonDep("org.apache.ant", "ant"))
    api(project(":kotlin-preloader"))
    api(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar {
    manifest.attributes["Class-Path"] = "$compilerManifestClassPath kotlin-preloader.jar"
}
