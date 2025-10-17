plugins {
    kotlin("jvm")
    id("java-test-fixtures")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.interpreter"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":compiler:ir.validation"))
    compileOnly(project(":compiler:frontend")) // this dependency is needed because of `IrPluginContext` exposing K1 frontend as deprecated.
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

