plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:serialization"))
    api(project(":compiler:ir.serialization.data"))
    api(project(":kotlin-util-klib"))
    api(project(":kotlin-util-klib-metadata"))
    api(project(":compiler:util"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(libs.intellij.fastutil)
}

optInToUnsafeDuringIrConstructionAPI()
optInToObsoleteDescriptorBasedAPI()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
