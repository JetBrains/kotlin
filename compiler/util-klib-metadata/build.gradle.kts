plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Common klib metadata reader and writer"

dependencies {
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":core:deserialization"))
    compileOnly(project(":compiler:serialization"))

    compile(kotlinStdlib())
    compile(project(":kotlin-util-io"))
    compile(project(":kotlin-util-klib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

publish()

standardPublicJars()
