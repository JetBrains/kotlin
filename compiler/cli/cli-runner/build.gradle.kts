
description = "Kotlin Runner"

apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

jvmTarget = "1.6"

dependencies {
    compile(projectDist(":kotlin-stdlib"))
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

