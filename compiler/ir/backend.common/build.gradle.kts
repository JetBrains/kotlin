plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("test-inputs-check")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.interpreter"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":compiler:ir.validation"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":compiler:frontend.common-psi")) // required for error reporting
    compileOnly(intellijCore())

    testImplementation(kotlinTest("junit"))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))

    testFixturesImplementation(kotlinTest("junit"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

