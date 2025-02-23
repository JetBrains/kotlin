plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:backend-common"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.interpreter"))
    implementation(project(":compiler:config.jvm"))
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

