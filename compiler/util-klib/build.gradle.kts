plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Common klib reader and writer"

dependencies {
    compile(kotlinStdlib())
    compile(project(":kotlin-util-io"))
    testImplementation(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

publish()

standardPublicJars()