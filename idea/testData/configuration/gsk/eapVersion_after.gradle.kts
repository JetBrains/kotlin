import org.gradle.api.JavaVersion.VERSION_1_7

val kotlin_version: String by extra
buildscript {
    var kotlin_version: String by extra
    kotlin_version = "$VERSION$"
    repositories {
        maven {
            setUrl("http://dl.bintray.com/kotlin/kotlin-eap")
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
        setUrl("http://dl.bintray.com/kotlin/kotlin-eap")
    }
}

dependencies {
    testCompile("junit:junit:4.12")
    compile(kotlinModule("stdlib", kotlin_version))
}

// VERSION: $VERSION$