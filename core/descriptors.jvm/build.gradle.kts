plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"
javaHome = rootProject.extra["JDK_16"] as String

dependencies {
    compile(project(":kotlin-annotations-jvm"))
    compile(project(":core:descriptors"))
    compile(project(":core:deserialization"))
    compile(project(":core:metadata.jvm"))
    compile(project(":core:util.runtime"))
    compile(commonDep("javax.inject"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.6"
    targetCompatibility = "1.6"
}
