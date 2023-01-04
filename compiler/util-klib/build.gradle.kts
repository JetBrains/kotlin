plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Common klib reader and writer"

dependencies {
    api(kotlinStdlib())
    api(project(":kotlin-util-io"))
    // HACK compileOnly to avoid creating invalid dependencies in kotlin-util-klib maven artifact
    // (unfortunatelly, we use this hack everywhere in the Kotlin repo :( Something should be done with this problem globally)
    compileOnly(project(":core:metadata.buildSrc"))
    testImplementation(commonDependency("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

publish()

standardPublicJars()