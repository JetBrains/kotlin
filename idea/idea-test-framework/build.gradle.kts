
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(project(":compiler:frontend"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(project(":idea"))
    testCompile(project(":idea:idea-jvm"))
    testCompile(project(":idea:idea-core"))
    testCompile(project(":idea:idea-jps-common"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testCompile(commonDep("junit:junit"))
    testCompileOnly(intellijDep())
    Platform[192].orHigher {
        testCompileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl", "external-system-rt", "external-system-impl") }
    }
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar()