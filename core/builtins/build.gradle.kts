import org.gradle.jvm.tasks.Jar
import java.io.File

val builtinsSrc = fileFrom(rootDir, "core", "builtins", "src")
val builtinsNative = fileFrom(rootDir, "core", "builtins", "native")
// TODO: rewrite dependent projects on using build results instead of the fixed location
val builtinsSerialized = File(rootProject.extra["distDir"].toString(), "builtins")

val builtins by configurations.creating

val serialize = task<Exec>("serialize") {
    val outDir = builtinsSerialized
    val inDirs = arrayOf(builtinsSrc, builtinsNative)
    outputs.dir(outDir)
    inputs.files(*inDirs)
    commandLine(
        "java", "-cp",
        rootProject.buildscript.configurations["bootstrapCompilerClasspath"].asPath,
        "org.jetbrains.kotlin.serialization.builtins.RunKt",
        "$outDir", *inDirs
    )
}

val builtinsJar by task<Jar> {
    dependsOn(serialize)
    from(builtinsSerialized) { include("kotlin/**") }
    baseName = "platform-builtins"
    destinationDir = File(buildDir, "libs")
}

artifacts.add(builtins.name, builtinsJar)
