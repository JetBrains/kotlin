
description = "Kotlin Ant Tools"

plugins {
    kotlin("jvm")
}

dependencies {
    compile(commonDep("org.apache.ant", "ant"))
    compile(project(":kotlin-preloader"))
    compile(project(":kotlin-stdlib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar {
    from("$projectDir/src") { include("**/*.xml") }
    manifest.attributes.put("Class-Path", "$compilerManifestClassPath kotlin-preloader.jar")
}

dist()

