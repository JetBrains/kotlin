
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(project(":compiler:frontend"))
    testCompile(project(":compiler:frontend.script"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(project(":idea"))
    testCompile(project(":idea:idea-jvm"))
    testCompile(project(":idea:idea-core"))
    testCompile(project(":idea:idea-jps-common"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testCompile(commonDep("junit:junit"))
    testCompileOnly(intellijDep()) { includeJars("openapi", "idea", "log4j") }
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar()