plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("test-inputs-check")
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    api(project(":compiler:util"))
    api(project(":core:compiler.common"))
    api(project(":kotlin-stdlib"))
    api(project(":compiler:ir.tree"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:util.runtime"))
    implementation(project(":kotlin-util-klib"))

    compileOnly(intellijCore())

    testImplementation(project.kotlinTest("junit5"))
    testRuntimeOnly(testFixtures(project(":compiler:tests-common-new")))
    testImplementation(project(":compiler:ir.backend.common"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}