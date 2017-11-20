import org.gradle.api.JavaVersion.VERSION_1_7

buildscript {
    repositories {
        mavenCentral()
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
}

dependencies {
    testCompile("junit:junit:4.12")
}
