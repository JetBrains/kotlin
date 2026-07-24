plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("test-inputs-check-v2")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":compiler:ir.validation"))
    implementation(project(":compiler:resolution"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":core:descriptors"))
    implementation(project(":compiler:frontend.common-psi")) // required for error reporting
    compileOnly(intellijCore())

    testImplementation(kotlinTest("junit"))

    testFixturesImplementation(kotlinTest("junit"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

