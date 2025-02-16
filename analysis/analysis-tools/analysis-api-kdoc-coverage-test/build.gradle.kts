plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(projectTests(":compiler"))
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
    }
}

projectTest {
    workingDir = rootDir
}
