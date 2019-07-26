plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Common klib metadata reader and writer"

dependencies {
    compile(kotlinStdlib())
    compile(project(":compiler:frontend"))
    compile(project(":core:deserialization"))
    compile(project(":compiler:serialization"))
    compile(project(":kotlin-util-io"))
    compile(project(":kotlin-util-klib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

publish()

standardPublicJars()
