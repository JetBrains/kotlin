plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    maven(url = "https://dl.bintray.com/kotlin/kotlinx")
}

dependencies {
    compile(project(":core:compiler.common"))
    compile(project(":core:descriptors.jvm"))
    compile(project(":core:deserialization"))
    compile(project(":compiler:fir:cones"))
    compile(project(":compiler:fir:tree"))
    compile(project(":compiler:resolution.common"))
    compile("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:${property("versions.kotlinx-collections-immutable")}")
    implementation(project(":core:util.runtime"))

    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCoreDep()) { includeJars("guava", rootProject = rootProject) }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
