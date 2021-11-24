plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:ir.psi2ir"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":js:js.frontend"))
    api(project(":compiler:ir.tree.persistent"))

    implementation(project(":compiler:ir.backend.common"))
    compileOnly(project(":kotlin-reflect-api"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
}
