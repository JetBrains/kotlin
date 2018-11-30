import org.gradle.jvm.tasks.Jar
import java.io.File

plugins {
    base
}

val builtinsSrc = fileFrom(rootDir, "core", "builtins", "src")
val builtinsNative = fileFrom(rootDir, "core", "builtins", "native")
// TODO: rewrite dependent projects on using build results instead of the fixed location
val builtinsSerialized = File(rootProject.extra["distDir"].toString(), "builtins")

val builtins by configurations.creating

val clean by tasks.getting {
    doLast {
        delete(builtinsSerialized)
    }
}

val serialize by tasks.creating(UtilityJavaExec::class) {
    val outDir = builtinsSerialized
    val inDirs = arrayOf(builtinsSrc, builtinsNative)
    inDirs.forEach { inputs.dir(it) }
    outputs.dir(outDir)

    classpath(rootProject.buildscript.configurations["bootstrapCompilerClasspath"])
    main = "org.jetbrains.kotlin.serialization.builtins.RunKt"
    jvmArgs("-Didea.io.use.nio2=true")
    args(outDir, *inDirs)
}

val builtinsJar by task<Jar> {
    dependsOn(serialize)
    from(builtinsSerialized) { include("kotlin/**") }
    baseName = "platform-builtins"
    destinationDir = File(buildDir, "libs")
}


val assemble by tasks.getting {
    dependsOn(serialize)
}

artifacts.add(builtins.name, builtinsJar)
