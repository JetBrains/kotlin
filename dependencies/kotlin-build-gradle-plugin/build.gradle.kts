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
version = "0.0.42"

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    compileOnly(kotlin("stdlib"))
}

kotlin.jvmToolchain(17)

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
            credentials(PasswordCredentials::class)
        }
    }
}
