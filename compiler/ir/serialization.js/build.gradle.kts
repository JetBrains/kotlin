plugins {
    kotlin("jvm")
    id("test-inputs-check")
    id("project-tests-convention")
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5)
}

dependencies {
    implementation(project(":compiler:ir.psi2ir"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:ir.serialization.common"))
    implementation(project(":js:js.frontend"))

    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:fir:fir-serialization"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:serialization"))
    implementation(project(":js:js.config"))
    implementation(project(":wasm:wasm.config"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))
    implementation(project(":core:compiler.common.wasm"))
    implementation(project(":core:compiler.common.js"))
    implementation(project(":js:js.parser"))
    implementation(project(":kotlin-util-klib-metadata"))

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

optInToK1Deprecation()

