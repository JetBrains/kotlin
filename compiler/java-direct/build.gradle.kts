plugins {
    kotlin("jvm")
    id("test-inputs-check")
}

dependencies {
    api(project(":compiler:frontend.java"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
