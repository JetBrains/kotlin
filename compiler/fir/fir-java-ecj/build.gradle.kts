plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:fir:tree"))
    api(project(":compiler:fir:fir-jvm"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:fir-deserialization"))
    implementation("org.eclipse.jdt:ecj:3.41.0")

    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_17_0))
    workingDir = rootDir
    useJUnitPlatform()
}

testsJar()
