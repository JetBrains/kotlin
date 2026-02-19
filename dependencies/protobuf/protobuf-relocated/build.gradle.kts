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

val baseProtobuf by configurations.creating
val baseProtobufSources by configurations.creating
val protobufPatches by configurations.creating

val protobufVersion: String by rootProject.extra
val protobufJarPrefix = "protobuf-$protobufVersion"

val renamedSources = "$buildDir/renamedSrc/"
val outputJarsPath = "$buildDir/libs"

dependencies {
    baseProtobuf("com.google.protobuf:protobuf-java:$protobufVersion")
    protobufPatches(project(":protobuf-patches"))
    baseProtobufSources("com.google.protobuf:protobuf-java:$protobufVersion:sources")
}

val prepare = tasks.register<ShadowJar>("shadow") {
    dependsOn(":protobuf-patches:build")
    destinationDirectory.set(File(outputJarsPath))
    archiveVersion.set(protobufVersion)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(zipTree(protobufPatches.resolvedConfiguration.resolvedArtifacts.single { it.name == "protobuf-patches" }.file))
    from(zipTree(baseProtobuf.files.find { it.name.startsWith("protobuf-java") }!!.canonicalPath))

    relocate("com.google.protobuf", "org.jetbrains.kotlin.protobuf" ) {
        exclude("META-INF/maven/com.google.protobuf/protobuf-java/pom.properties")
    }
}

artifacts.add("default", prepare)

val relocateSources = task<Copy>("relocateSources") {
    dependsOn(":protobuf-patches:build")
    from(
        provider {
            zipTree(baseProtobufSources.files.find { it.name.startsWith("protobuf-java") && it.name.endsWith("-sources.jar") }
                        ?: throw GradleException("sources jar not found among ${baseProtobufSources.files}"))
        }
    ) {
        exclude("com/google/protobuf/CodedInputStream.java")
    }

    from(project(":protobuf-patches").extensions.getByType(SourceSetContainer::class.java).getByName("main").allSource.srcDirs)

    into(renamedSources)

    filter { it.replace("com.google.protobuf", "org.jetbrains.kotlin.protobuf") }
}

val prepareSources = task<Jar>("prepareSources") {
    destinationDirectory.set(File(outputJarsPath))
    archiveVersion.set(protobufVersion)
    archiveClassifier.set("sources")
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
        maven {
            name = "kotlinSpace"
            url = uri("https://packages.jetbrains.team/maven/p/kt/kotlin-dependencies")
            credentials(org.gradle.api.artifacts.repositories.PasswordCredentials::class)
        }
    }
}
