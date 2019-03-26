plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij") version "0.4.5"
}

group = "org.jetbrains.kotlin.fir"

repositories {
    mavenCentral()
    mavenLocal()
    maven { setUrl("https://www.jetbrains.com/intellij-repository/releases") }
    maven { setUrl("https://jetbrains.bintray.com/intellij-third-party-dependencies") }
}

dependencies {
    compile("com.jetbrains.intellij.java:java-psi-impl:183.5912.17")
    compile("com.jetbrains.intellij.java:java-psi:183.5912.17")
    
    compile(project(":compiler:psi"))
    
    compile("junit", "junit", "4.4")
    compile(projectTests(":compiler:fir:psi2fir"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}
