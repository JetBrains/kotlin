plugins {
    kotlin("jvm")
    id("test-inputs-check")
    id("project-tests-convention")
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5)
}

dependencies {
    api(project(":compiler:util"))
    api(project(":core:compiler.common"))
    api(project(":kotlin-stdlib"))
    api(project(":compiler:ir.tree"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:util.runtime"))
    implementation(project(":kotlin-util-klib"))

    compileOnly(intellijCore())

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(testFixtures(project(":compiler:tests-common-new")))
    testImplementation(testFixtures(project(":compiler:ir.tree")))
    testImplementation(project(":compiler:ir.backend.common"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
