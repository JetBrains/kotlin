
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
    manifest.attributes.put("Class-Path", "$compilerManifestClassPath kotlin-preloader.jar")
}

dist()

