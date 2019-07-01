import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.File

plugins {
    `java-base`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "4.0.3" apply false
}

repositories {
    jcenter()
}

val baseProtobuf by configurations.creating
val baseProtobufSources by configurations.creating

val protobufVersion: String by rootProject.extra
val protobufJarPrefix = "protobuf-$protobufVersion"

val renamedSources = "$buildDir/renamedSrc/"
val outputJarsPath = "$buildDir/libs"

dependencies {
    baseProtobuf("com.google.protobuf:protobuf-java:$protobufVersion")
    baseProtobufSources("com.google.protobuf:protobuf-java:$protobufVersion:sources")
}

val prepare = task<ShadowJar>("prepare") {
    destinationDir = File(outputJarsPath)
    version = protobufVersion
    classifier = ""
    from(
        provider {
            baseProtobuf.files.find { it.name.startsWith("protobuf-java") }?.canonicalPath
        }
    )

    relocate("com.google.protobuf", "org.jetbrains.kotlin.protobuf" ) {
        exclude("META-INF/maven/com.google.protobuf/protobuf-java/pom.properties")
    }
}

artifacts.add("default", prepare)

val relocateSources = task<Copy>("relocateSources") {
    from(
        provider {
            zipTree(baseProtobufSources.files.find { it.name.startsWith("protobuf-java") && it.name.endsWith("-sources.jar") }
                        ?: throw GradleException("sources jar not found among ${baseProtobufSources.files}"))
        }
    )

    into(renamedSources)

    filter { it.replace("com.google.protobuf", "org.jetbrains.kotlin.protobuf") }
}

val prepareSources = task<Jar>("prepareSources") {
    destinationDir = File(outputJarsPath)
    version = protobufVersion
    classifier = "sources"
    from(relocateSources)
}

artifacts.add("default", prepareSources)

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
    }
}
