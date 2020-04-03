plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testRuntime(intellijDep())
    testCompile(intellijCoreDep()) { includeJars("intellij-core") }

    testCompileOnly(project(":compiler:fir:raw-fir:psi2fir"))

    testCompileOnly(project(":compiler:visualizer:render-psi"))
    testCompileOnly(project(":compiler:visualizer:render-fir"))

    testCompile(commonDep("junit:junit"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":compiler:fir:analysis-tests"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

testsJar()