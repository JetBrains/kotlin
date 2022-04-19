plugins {
    kotlin("jvm")
    id("jps-compatible")
}

kotlin {
    explicitApiWarning()
}

dependencies {
    implementation(intellijCore())
    implementation(project(":compiler:psi"))
    implementation(project(":compiler:cli"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-providers"))
    api(project(":analysis:project-structure"))
    api(project(":analysis:analysis-api-fir"))
    api(project(":analysis:low-level-api-fir"))
    api(project(":analysis:symbol-light-classes"))
    api(project(":analysis:decompiled:light-classes-for-decompiled"))

    testApi(projectTests(":analysis:analysis-api-impl-base"))
    testApi(projectTests(":analysis:analysis-api-fir"))

    testApi(project(":kotlin-test:kotlin-test-junit"))
    testApi(toolsJar())
    testApiJUnit5()
}

kotlin {
    explicitApi()
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
}.also { confugureFirPluginAnnotationsDependency(it) }

testsJar()

