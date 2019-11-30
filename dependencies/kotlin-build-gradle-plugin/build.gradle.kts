plugins {
    kotlin("jvm") version embeddedKotlinVersion
    `maven-publish`
}

group = "org.jetbrains.kotlin"
version = "0.0.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
    }
}

publishing {
    publications {
        create<MavenPublication>("KotlinBuildGradlePlugin") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "bintray"
            url = uri("https://api.bintray.com/maven/kotlin/kotlin-dependencies/kotlin-build-gradle-plugin")
            authentication {
                val mavenUser = findProperty("kotlin.bintray.user") as String?
                val mavenPass = findProperty("kotlin.bintray.password") as String?
                if (mavenUser != null && mavenPass != null) {
                    credentials {
                        username = mavenUser
                        password = mavenPass
                    }
                }
            }
        }
    }
}
