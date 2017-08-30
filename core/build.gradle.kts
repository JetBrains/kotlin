
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply { plugin("kotlin") }

jvmTarget = "1.6"
javaHome = rootProject.extra["JDK_16"] as String

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project("util.runtime"))
    compile(protobufLite())
    compile(commonDep("javax.inject"))
}

sourceSets {
    "main" {
        java.srcDirs("descriptor.loader.java/src",
                     "descriptors/src",
                     "descriptors.runtime/src",
                     "deserialization/src")
        resources.srcDirs("descriptor.loader.java/src", "deserialization/src").apply { include("META-INF/**") }
    }
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


