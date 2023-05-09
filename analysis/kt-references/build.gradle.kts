plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":compiler:psi"))
    implementation(project(":analysis:light-classes-base"))
    implementation(intellijCore())
    implementation(project(":analysis:analysis-api-providers"))
    implementation(project(":analysis:project-structure"))

    compileOnly(commonDependency("com.google.guava:guava"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
