
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(intellijDep()) { includeJars("util") }
    testCompile(project(":core:util.runtime"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testCompile(kotlinStdlib())
    testCompile(commonDep("junit:junit"))
    testCompile(project(":generators"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}
