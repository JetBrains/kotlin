
apply { plugin("kotlin") }

dependencies {
    testCompile(projectTests(":compiler:tests-common"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}
