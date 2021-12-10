plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:psi"))
    api(project(":core:deserialization.common"))
    api(project(":core:deserialization.common.jvm"))
    api(project(":core:deserialization"))
    implementation(project(":core:compiler.common.jvm"))

    api(intellijCoreDep()) { includeJars("intellij-core", rootProject = rootProject) }

}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}


