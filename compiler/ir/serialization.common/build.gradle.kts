plugins {
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
    id("java-test-fixtures")
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:serialization"))
    api(project(":kotlin-util-klib"))
    api(project(":kotlin-util-klib-metadata"))
    api(project(":compiler:util"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:psi:psi-frontend-utils"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(libs.intellij.fastutil)

    compileOnly(intellijCore())
    compileOnly(project(":compiler:cli-base"))

    testFixturesApi(libs.junit.jupiter.api)
    testFixturesImplementation(testFixtures(project(":compiler:ir.tree")))
}

optInToUnsafeDuringIrConstructionAPI()
optInToObsoleteDescriptorBasedAPI()

sourceSets {
    "main" { projectDefault() }
    "test" {}
    "testFixtures" { projectDefault() }
}
