plugins {
    id("common-configuration")
    id("test-federation-convention")
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

dependencies {
    implementation(kotlinStdlib())
    implementation(project(":kotlin-tooling-core"))
    testImplementation(kotlinTest("junit5"))
    testImplementation(platform(libs.junit.bom))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        useJUnitPlatform()
    }
}
