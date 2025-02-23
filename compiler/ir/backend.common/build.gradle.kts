plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:backend-common"))
    implementation(project(":compiler:ir.tree"))
    implementation(project(":compiler:ir.interpreter"))
    implementation(project(":kotlin-util-klib"))
    compileOnly(intellijCore())

    testImplementation(kotlinTest("junit"))
    testImplementation(projectTests(":compiler:tests-common-new"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

