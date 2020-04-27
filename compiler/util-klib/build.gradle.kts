plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Common klib reader and writer"

dependencies {
    compile(kotlinStdlib())
    compile(project(":kotlin-util-io"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

publish()

standardPublicJars()