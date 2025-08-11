plugins {
    kotlin("jvm") version embeddedKotlinVersion
    `maven-publish`
}

group = "org.jetbrains.kotlin"

/*
How to Publish

1. Bump version parameter
2. Prepare publication credentials for https://jetbrains.team/p/kt/packages/maven/kotlin-dependencies/org.jetbrains.kotlin/kotlin-build-gradle-plugin
3. Execute `./gradlew -p dependencies/kotlin-build-gradle-plugin publish -PkotlinSpaceUsername=usr -PkotlinSpacePassword=token`
 */
version = "0.0.40"

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
}

tasks.compileJava {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
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
        /*TODO: move version to build-plugin*/
        java.setSrcDirs(listOf("src", "../../compiler/util-io/src"))
    }
}

tasks.withType<GenerateModuleMetadata> {
    enabled = false
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
            url = uri("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
            credentials(org.gradle.api.artifacts.repositories.PasswordCredentials::class)
        }
    }
}
