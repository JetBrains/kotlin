plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":compiler:psi"))
    implementation(kotlinxCollectionsImmutable())
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    implementation(project(":analysis:project-structure"))
    implementation(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

kotlin {
    explicitApi()
}