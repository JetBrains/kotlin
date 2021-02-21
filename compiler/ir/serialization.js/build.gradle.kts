plugins {
    kotlin("jvm")
    id("jps-compatible")
}

// Please make sure this module doesn't depend on `backend.js` (neither directly, nor transitively)
dependencies {
    compile(project(":compiler:ir.psi2ir"))
    compile(project(":compiler:ir.serialization.common"))
    compile(project(":js:js.frontend"))
    implementation(project(":compiler:ir.backend.common"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
}
