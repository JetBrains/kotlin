plugins {
    kotlin("jvm")
    id("test-inputs-check")
    id("project-tests-convention")
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5)
}

dependencies {
    api(project(":compiler:ir.psi2ir"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":js:js.frontend"))

    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:fir:fir-serialization"))
    implementation(project(":wasm:wasm.config"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":core:compiler.common.wasm"))

    compileOnly(intellijCore())

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(testFixtures(project(":compiler:tests-common-new")))
    testImplementation(testFixtures(project(":compiler:ir.serialization.common")))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
