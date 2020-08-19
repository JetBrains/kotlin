plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"
javaHome = rootProject.extra["JDK_16"] as String

dependencies {
    compile(project(":core:descriptors.common"))
    compile(project(":core:util.runtime"))
    compile(project(":core:type-system"))
    compile(kotlinStdlib())
    compile(project(":kotlin-annotations-jvm"))
    api(project(":core:deserialization:deserialization.common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.6"
    targetCompatibility = "1.6"
}
