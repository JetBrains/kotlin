import org.gradle.api.JavaVersion.VERSION_1_7
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlin_version: String by extra
buildscript {
    var kotlin_version: String by extra
    kotlin_version = "$VERSION$"
    repositories {
        maven {
            setUrl("http://dl.bintray.com/kotlin/kotlin-eap-1.1")
        }
    }
    dependencies {
        classpath(kotlinModule("gradle-plugin", kotlin_version))
    }
}

plugins {
    application
}
apply {
    plugin("kotlin")
}

application {
    mainClassName = "samples.HelloWorld"
}

repositories {
    jcenter()
    maven {
        setUrl("http://dl.bintray.com/kotlin/kotlin-eap-1.1")
    }
}

dependencies {
    testCompile("junit:junit:4.12")
    compile(kotlinModule("stdlib-jre8", kotlin_version))
}

// VERSION: $VERSION$
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}