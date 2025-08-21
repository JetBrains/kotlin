import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.File

plugins {
    `java-base`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "4.0.3" apply false
}

repositories {
    mavenCentral()
}

val relocatedProtobuf by configurations.creating
val relocatedProtobufSources by configurations.creating

val protobufVersion: String by rootProject.extra

val renamedSources = "${layout.buildDirectory.get()}/renamedSrc/"
val outputJarsPath = "${layout.buildDirectory.get()}/libs"

dependencies {
    relocatedProtobuf("com.google.protobuf:protobuf-javalite:$protobufVersion") { isTransitive = false }
    relocatedProtobufSources("com.google.protobuf:protobuf-javalite:$protobufVersion:sources") { isTransitive = false }
}

val prepare = tasks.register<ShadowJar>("prepare") {
    destinationDirectory.set(File(outputJarsPath))
    archiveVersion.set(protobufVersion)
    archiveClassifier.set("")
    from(relocatedProtobuf)

    relocate("com.google.protobuf", "org.jetbrains.kotlin.protobuf" ) {
        exclude("META-INF/maven/com.google.protobuf/protobuf-javalite/pom.properties")
    }
}

val relocateSources = task<Copy>("relocateSources") {
    from(
        provider {
            zipTree(relocatedProtobufSources.files.single())
        }
    )

    into(renamedSources)

    filter { it.replace("com.google.protobuf", "org.jetbrains.kotlin.protobuf") }
}

val prepareSources = task<Jar>("prepareSources") {
    destinationDirectory.set(File(outputJarsPath))
    archiveVersion.set(protobufVersion)
    archiveClassifier.set("sources")
    from(relocateSources)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(prepare)
            artifact(prepareSources)
        }
    }

    repositories {
        maven {
            url = uri("${rootProject.buildDir}/internal/repo")
        }

        maven {
            name = "kotlinSpace"
            url = uri("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
            credentials(org.gradle.api.artifacts.repositories.PasswordCredentials::class)
        }
    }
}
