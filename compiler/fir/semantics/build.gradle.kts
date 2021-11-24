plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:fir:providers"))
    implementation(project(":core:util.runtime"))

    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(commonDependency("com.google.guava:guava"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
