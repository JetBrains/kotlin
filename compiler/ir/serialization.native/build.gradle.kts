plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("test-inputs-check-v2")
    id("project-tests-convention")
}

projectTests {
    testTask()
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.serialization.common"))

    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":native:native.config"))
    runtimeOnly(project(":native:frontend.native"))
    implementation(project(":compiler:fir:fir2ir"))
    implementation(project(":compiler:fir:tree"))
    implementation(project(":compiler:ir.objcinterop"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":core:descriptors"))
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
