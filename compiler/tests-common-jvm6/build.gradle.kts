plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.updateJvmTarget("1.6") // Should this project be removed altogether ?

dependencies {
    api(kotlinStdlib())
    testApi(project(":kotlin-test:kotlin-test-jvm"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}
