plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":compiler:config"))
    implementation(project(":compiler:ir.tree"))
    implementation(project(":compiler:serialization"))
    api(project(":kotlin-util-klib"))
    api(project(":kotlin-util-klib-metadata"))
    implementation(project(":compiler:util"))
    implementation(project(":compiler:frontend.common-psi"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(libs.intellij.fastutil)

    compileOnly(intellijCore())
    compileOnly(project(":compiler:cli-common"))
}

optInToUnsafeDuringIrConstructionAPI()
optInToObsoleteDescriptorBasedAPI()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
