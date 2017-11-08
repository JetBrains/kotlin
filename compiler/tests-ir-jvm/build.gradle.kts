
apply { plugin("kotlin") }

dependencies {
    testCompile(projectTests(":compiler:tests-common"))
    testRuntime(projectTests(":compiler"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}
