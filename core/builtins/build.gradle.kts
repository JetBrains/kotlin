import org.gradle.jvm.tasks.Jar
import java.io.File

plugins {
    base
    `maven-publish`
}

val builtinsSrc = fileFrom(rootDir, "core", "builtins", "src")
val builtinsNative = fileFrom(rootDir, "core", "builtins", "native")
val kotlinReflect = fileFrom(rootDir, "libraries/stdlib/src/kotlin/reflect")
val builtinsCherryPicked = fileFrom(buildDir, "src")

val prepareSources by tasks.registering(Sync::class) {
    from(kotlinReflect) {
        exclude("typeOf.kt")
        exclude("KClasses.kt")
    }
    into(builtinsCherryPicked)
}

val serialize by tasks.registering(NoDebugJavaExec::class) {
    dependsOn(prepareSources)
    val outDir = "$buildDir/$name"
    val inDirs = arrayOf(builtinsSrc, builtinsNative, builtinsCherryPicked)
    inDirs.forEach { inputs.dir(it) }
    outputs.dir(outDir)

    classpath(rootProject.buildscript.configurations["bootstrapCompilerClasspath"])
    main = "org.jetbrains.kotlin.serialization.builtins.RunKt"
    jvmArgs("-Didea.io.use.nio2=true")
    args(outDir, *inDirs)
}

val builtinsJar by task<Jar> {
    dependsOn(serialize)
    from(serialize) { include("kotlin/**") }
    destinationDir = File(buildDir, "libs")
}

val assemble by tasks.getting {
    dependsOn(serialize)
}

val builtinsJarArtifact = artifacts.add("default", builtinsJar)

publishing {
    publications {
        create<MavenPublication>("internal") {
            artifact(builtinsJarArtifact)
        }
    }

    repositories {
        maven("${rootProject.buildDir}/internal/repo")
    }
}
