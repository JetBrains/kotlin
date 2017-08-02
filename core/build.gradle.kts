
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":core:builtins"))
    compile(project("util.runtime"))
    compile(protobufLite())
    compile(commonDep("javax.inject"))
}

configureKotlinProjectSources(
        "descriptor.loader.java/src",
        "descriptors/src",
        "descriptors.runtime/src",
        "deserialization/src")
configureKotlinProjectResources(
        "descriptor.loader.java/src", "deserialization/src") { include("META-INF/**") }
configureKotlinProjectNoTests()

tasks.withType<JavaCompile> {
    dependsOn(protobufLiteTask)
}

tasks.withType<KotlinCompile> {
    dependsOn(protobufLiteTask)
}


