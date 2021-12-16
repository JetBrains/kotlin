plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:metadata.jvm"))
    api(project(":core:deserialization.common"))
    api(project(":core:deserialization.common.jvm"))
    implementation(project(":core:compiler.common.jvm"))
    api(intellijCoreDep()) { includeJars("intellij-core", rootProject = rootProject) }
    implementation(intellijDep()) { includeJars("asm-all", rootProject = rootProject) }

    implementation(project(":core:descriptors.jvm"))

}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
