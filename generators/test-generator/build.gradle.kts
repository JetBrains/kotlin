
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(intellijDep()) { includeJars("util") }
    testCompile(project(":core:util.runtime"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(kotlinStdlib())
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}
