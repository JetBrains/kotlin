plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:ir.tree"))
    implementation(project(":kotlin-util-klib"))

    compileOnly(intellijCore())

    testImplementation(kotlinTest("junit"))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}