plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:serialization"))
    api(project(":kotlin-util-klib"))
    api(project(":kotlin-util-klib-metadata"))
    api(project(":compiler:util"))
    implementation(project(":compiler:psi"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))

    compileOnly(intellijCore())
}

optInToIrSymbolInternals()
optInToObsoleteDescriptorBasedAPI()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
