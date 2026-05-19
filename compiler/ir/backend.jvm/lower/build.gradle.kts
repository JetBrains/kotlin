plugins {
    kotlin("jvm")
    id("test-inputs-check")
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:ir.inline"))
    api(project(":compiler:backend.jvm"))
    implementation(project(":compiler:backend.common.jvm"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:psi:psi-frontend-utils"))
    implementation(project(":compiler:resolution"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    compileOnly(intellijCore())

    testImplementation(kotlinTest("junit"))
    testImplementation(testFixtures(project(":compiler:ir.backend.common")))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
