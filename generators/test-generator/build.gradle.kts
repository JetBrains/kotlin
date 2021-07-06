
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(intellijDep()) { includeJars("util") }
    testApi(project(":core:util.runtime"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(kotlinStdlib())
    testApi(commonDep("junit:junit"))
    testApi(project(":generators"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}
