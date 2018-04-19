
description = "Kotlin Ant Tools"

plugins {
    kotlin("jvm")
}

dependencies {
    compile(commonDep("org.apache.ant", "ant"))
    compile(project(":kotlin-preloader"))
    compile(projectDist(":kotlin-stdlib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar {
    from("$projectDir/src") { include("**/*.xml") }
    manifest.attributes.put("Class-Path", "kotlin-stdlib.jar kotlin-reflect.jar kotlin-script-runtime.jar kotlin-preloader.jar")
}

dist()

