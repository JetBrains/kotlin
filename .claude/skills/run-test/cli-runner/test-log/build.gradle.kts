import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    // Source: https://mvnrepository.com/artifact/org.junit.platform/junit-platform-engine
    implementation("org.junit.platform:junit-platform-engine:1.14.3")
    // Source: https://mvnrepository.com/artifact/org.junit.platform/junit-platform-launcher
    implementation("org.junit.platform:junit-platform-launcher:1.14.3")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "8"
        targetCompatibility = "8"
    }
    withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
    }
}
