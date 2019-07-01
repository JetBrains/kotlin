
description = "Kotlin Ant Tools"

plugins {
    kotlin("jvm")
}

dependencies {
    compile(commonDep("org.apache.ant", "ant"))
    compile(project(":kotlin-preloader"))
    compile(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar {
    manifest.attributes["Class-Path"] = "$compilerManifestClassPath kotlin-preloader.jar"
}
