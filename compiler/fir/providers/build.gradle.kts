plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":compiler:resolution.common"))
    api(project(":compiler:fir:cones"))
    api(project(":compiler:fir:tree"))
    implementation(project(":core:util.runtime"))

    compileOnly(commonDependency("com.google.guava:guava"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
