plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":idea:idea-frontend-independent"))
    implementation(project(":idea:idea-frontend-api"))
    implementation(project(":idea:idea-frontend-fir"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}
