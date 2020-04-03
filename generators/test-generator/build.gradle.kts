
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(intellijDep()) { includeJars("util") }
    testCompile(project(":core:util.runtime"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(kotlinStdlib())
    testCompile(commonDep("junit:junit"))
    testCompile(project(":generators"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}
