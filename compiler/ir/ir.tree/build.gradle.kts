plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:descriptors"))
    api(project(":core:deserialization"))
    api(project(":compiler:frontend.common"))
    implementation(project(":compiler:util"))
    implementation(project(":compiler:config"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
