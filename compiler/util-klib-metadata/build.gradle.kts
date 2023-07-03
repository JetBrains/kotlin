plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Common klib metadata reader and writer"

dependencies {
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":core:deserialization"))
    compileOnly(project(":core:compiler.common.native"))
    compileOnly(project(":compiler:serialization"))

    api(kotlinStdlib())
    api(project(":kotlin-util-io"))
    api(project(":kotlin-util-klib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

publish()

standardPublicJars()
