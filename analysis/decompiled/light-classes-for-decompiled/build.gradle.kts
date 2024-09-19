plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:psi"))
    api(project(":analysis:decompiled:decompiler-to-psi"))
    api(project(":analysis:decompiled:decompiler-to-file-stubs"))
    api(project(":analysis:light-classes-base"))

    implementation(intellijCore())

    testImplementation(project(":analysis:symbol-light-classes"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":analysis:analysis-test-framework"))
    testImplementation(projectTests(":analysis:decompiled:decompiler-to-file-stubs"))
    testImplementation(projectTests(":analysis:analysis-api-standalone"))
    testImplementation(project(":analysis:analysis-api-standalone"))
    testImplementation(projectTests(":analysis:low-level-api-fir"))
    testImplementation(projectTests(":analysis:analysis-api-impl-base"))
    testImplementation(project(":analysis:decompiled:light-classes-for-decompiled-fe10"))

    testImplementation(kotlinTest("junit"))
    testCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
}.also { confugureFirPluginAnnotationsDependency(it) }


testsJar()
