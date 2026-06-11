plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:psi:psi-api"))
    implementation(kotlinxCollectionsImmutable())
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    implementation(intellijCore())
    implementation(project(":compiler:util"))
    implementation(project(":core:names"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

kotlin {
    explicitApi()
}
