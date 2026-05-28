plugins {
    kotlin("jvm")
    application
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    implementation(kotlinStdlib())
    implementation(project(":kotlin-tooling-core"))
    testImplementation(kotlinTest("junit5"))
    testImplementation(platform(libs.junit.bom))
}

application {
    mainClass.set("org.jetbrains.kotlin.buildtools.versioncoverage.MainKt")
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
