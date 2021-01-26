plugins {
    kotlin("jvm") version embeddedKotlinVersion
    `maven-publish`
}

group = "org.jetbrains.kotlin"
version = "0.0.21"

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

java {
    withSourcesJar()
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
            name = "kotlinSpace"
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
            credentials(org.gradle.api.artifacts.repositories.PasswordCredentials::class)
        }
    }
}
