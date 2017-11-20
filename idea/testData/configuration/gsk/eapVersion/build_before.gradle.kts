import org.gradle.api.JavaVersion.VERSION_1_7

plugins {
    application
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

// VERSION: 1.0.2-eap-14-IJ143-14