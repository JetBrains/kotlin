plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":compiler:config"))
    api(project(":compiler:util"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
