apply { plugin("kotlin") }

dependencies {
    testRuntime(intellijDep())
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectDist(":kotlin-script-runtime"))
    testCompile(projectDist(":kotlin-stdlib"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}
