import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    id("com.gradleup.shadow") version "9.0.0-beta12"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.ow2.asm:asm:9.7.1")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }
    jar {
        manifest {
            attributes("Main-Class" to "org.jetbrains.kotlin.test.runner.MainKt")
        }
    }
    shadowJar {
        archiveClassifier.set("all")
        manifest {
            attributes("Main-Class" to "org.jetbrains.kotlin.test.runner.MainKt")
        }
    }
}

ktlint {
    version.set("1.8.0")
}
