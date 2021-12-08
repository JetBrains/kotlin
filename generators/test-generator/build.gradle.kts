
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(intellijDep()) { includeJars("util") }
    testApi(project(":core:util.runtime"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(kotlinStdlib())
    testApi(commonDependency("junit:junit"))
    testApiJUnit5()
    testApi(project(":generators"))

    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":core:descriptors.runtime"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}
