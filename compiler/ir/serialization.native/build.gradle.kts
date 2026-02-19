plugins {
    kotlin("jvm")
    id("test-inputs-check")
    id("project-tests-convention")
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5)
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.serialization.common"))

    implementation(project(":native:native.config"))
    implementation(project(":native:frontend.native"))
    implementation(project(":compiler:fir:fir2ir"))
    implementation(project(":compiler:fir:tree"))
    implementation(project(":compiler:ir.objcinterop"))
    implementation(project(":core:compiler.common.native"))

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
