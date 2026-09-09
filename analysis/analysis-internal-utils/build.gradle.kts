plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":core:util.runtime"))
    implementation(project(":core:error.handling"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":analysis:analysis-api"))
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
