plugins {
    kotlin("jvm")
    id("test-inputs-check")
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:ir.inline"))
    api(project(":compiler:backend.jvm"))
    compileOnly(intellijCore())

    testImplementation(kotlinTest("junit"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
