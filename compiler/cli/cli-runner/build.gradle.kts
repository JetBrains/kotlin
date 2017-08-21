
description = "Kotlin Runner"

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":kotlin-stdlib"))
    buildVersion()
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar {
    manifest.attributes.put("Main-Class", "org.jetbrains.kotlin.runner.Main")
    manifest.attributes.put("Class-Path", "kotlin-runtime.jar")
}

dist()

