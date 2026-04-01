plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:resolution"))
    api(project(":core:deserialization"))
    api(project(":compiler:util"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
