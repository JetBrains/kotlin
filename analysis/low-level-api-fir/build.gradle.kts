plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:psi"))
    implementation(project(":analysis:project-structure"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:fir:fir2ir:jvm-backend"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":compiler:fir:resolve"))
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:semantics"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:checkers:checkers.jvm"))
    api(project(":compiler:fir:checkers:checkers.js"))
    api(project(":compiler:fir:java"))
    api(project(":compiler:backend.common.jvm"))
    api(project(":analysis:analysis-api-impl-barebone"))
    testApi(project(":analysis:analysis-api-fir"))
    implementation(project(":compiler:frontend.common"))
    implementation(project(":compiler:ir.psi2ir"))
    implementation(project(":compiler:fir:entrypoint"))
    implementation(project(":analysis:analysis-api-providers"))
    implementation(project(":analysis:analysis-internal-utils"))

    api(intellijCore())

    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:tests-common-new"))

    testImplementation("org.opentest4j:opentest4j:1.2.0")
    testApi(toolsJar())
    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:fir:analysis-tests:legacy-fir-tests"))
    testApi(projectTests(":analysis:analysis-api-impl-barebone"))
    testApi(project(":kotlin-test:kotlin-test-junit"))
    testApiJUnit5()
    testApi(project(":kotlin-reflect"))
    testImplementation(project(":analysis:symbol-light-classes"))

    testRuntimeOnly(project(":core:descriptors.runtime"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
}

allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
        kotlinOptions {
            freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.fir.symbols.SymbolInternals"
        }
    }
}

testsJar()

