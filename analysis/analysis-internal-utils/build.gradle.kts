plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:psi:psi-api"))
    implementation(kotlinxCollectionsImmutable())
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    implementation(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

kotlin {
    explicitApi()
}
