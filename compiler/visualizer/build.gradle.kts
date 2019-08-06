plugins {
    kotlin("jvm")
}

dependencies {
    testRuntime(intellijDep())
    
    testCompile(project(":compiler:visualizer:render-psi"))
    testCompile(project(":compiler:visualizer:render-fir"))
    testCompile(project(":compiler:visualizer:common"))
    
    testCompile(commonDep("junit:junit"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":compiler:fir:resolve"))

}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

testsJar()