plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testRuntime(intellijDep())
    testCompile(intellijCoreDep()) { includeJars("intellij-core") }
    
    testCompile(project(":compiler:visualizer:render-psi"))
    testCompile(project(":compiler:visualizer:render-fir"))
    testCompile(project(":compiler:visualizer:common"))
    
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