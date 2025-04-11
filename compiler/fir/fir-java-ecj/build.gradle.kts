plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.updateJvmTarget("17")

dependencies {
    api(project(":compiler:fir:tree"))
    api(project(":compiler:fir:fir-jvm"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:fir-deserialization"))
    implementation("org.eclipse.jdt:ecj:3.41.0")

    testApi(platform(libs.junit.bom))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testApi(libs.junit.platform.launcher)
    testApi(kotlinTest("junit5"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    })
    workingDir = rootDir
    useJUnitPlatform()
}
