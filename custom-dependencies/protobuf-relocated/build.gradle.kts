
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
    }
}

val baseProtobuf by configurations.creating
val baseProtobufSources by configurations.creating

val resultsCfg = configurations.create("default")
val resultsSourcesCfg = configurations.create("sources")

val protobufVersion = rootProject.extra["versions.protobuf-java"] as String
val protobufJarPrefix = "protobuf-$protobufVersion"

val renamedSources = "$buildDir/renamedSrc/"
val outputJarsPath = "$buildDir/libs"
val artifactBaseName = "protobuf-java-relocated"

dependencies {
    baseProtobuf("com.google.protobuf:protobuf-java:$protobufVersion")
    baseProtobufSources("com.google.protobuf:protobuf-java:$protobufVersion:sources")
}

val prepare by task<ShadowJar> {
    destinationDir = File(outputJarsPath)
    baseName = artifactBaseName
    version = protobufVersion
    classifier = ""
    from(baseProtobuf.files.find { it.name.startsWith("protobuf-java") }?.canonicalPath)

    relocate("com.google.protobuf", "org.jetbrains.kotlin.protobuf" ) {
        exclude("META-INF/maven/com.google.protobuf/protobuf-java/pom.properties")
    }
    addArtifact("archives", this, this)
    addArtifact(resultsCfg.name, this, this)
}

val relocateSources by task<Copy> {
    from(zipTree(baseProtobufSources.files.find { it.name.startsWith("protobuf-java") && it.name.endsWith("-sources.jar") }
                 ?: throw GradleException("sources jar not found among ${baseProtobufSources.files}")))
    into(renamedSources)
    filter { it.replace("com.google.protobuf", "org.jetbrains.kotlin.protobuf") }
}

val prepareSources by task<Jar> {
    destinationDir = File(outputJarsPath)
    baseName = artifactBaseName
    version = protobufVersion
    classifier = "sources"
    from(relocateSources)
    project.addArtifact("archives", this, this)
    addArtifact(resultsSourcesCfg.name, this, this)
}

val clean by task<Delete> {
    delete(buildDir)
}