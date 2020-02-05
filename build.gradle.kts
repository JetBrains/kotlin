plugins {
    kotlin("jvm") version "1.3.60" apply false
    id("org.jetbrains.dokka") version "0.10.0" apply false
    id("nebula.release") version "13.1.1"
}

val release = tasks.findByPath(":release")
release?.finalizedBy(project.getTasksByName("publish", true))

allprojects {
    group = "com.bnorm.power"
}

subprojects {
    repositories {
        mavenCentral()
        jcenter()
    }
}
