import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply { plugin("kotlin") }

jvmTarget = "1.6"
javaHome = rootProject.extra["JDK_16"] as String

dependencies {
    compile(project(":core:util.runtime"))
    compile(project(":core:descriptors"))
    compile(protobufLite())
    compile(commonDep("javax.inject"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.withType<JavaCompile> {
    dependsOn(protobufLiteTask)
    sourceCompatibility = "1.6"
    targetCompatibility = "1.6"
}

tasks.withType<KotlinCompile> {
    dependsOn(protobufLiteTask)
}
