
import org.jetbrains.kotlin.serialization.builtins.BuiltInsSerializer
import org.gradle.jvm.tasks.Jar
import java.io.File

val builtinsSrc = File(rootDir, "core", "builtins", "src")
val builtinsNative = File(rootDir, "core", "builtins", "native")
// TODO: rewrite dependent projects on using build results instead of the fixed location
val builtinsSerialized = File(rootProject.extra["distDir"].toString(), "builtins")

val builtins by configurations.creating

val serialize = task("serialize") {
    val outDir = builtinsSerialized
    val inDirs = arrayOf(builtinsSrc, builtinsNative)
    outputs.dir(outDir)
    inputs.files(*inDirs)
    doLast {
        BuiltInsSerializer(dependOnOldBuiltIns = false)
                .serialize(outDir, inDirs.asList(), listOf()) { totalSize, totalFiles ->
                    println("Total bytes written: $totalSize to $totalFiles files")
                }
    }
}

val builtinsJar by task<Jar> {
    dependsOn(serialize)
    from(builtinsSerialized) { include("kotlin/**") }
    baseName = "platform-builtins"
    destinationDir = File(buildDir, "libs")
}

artifacts.add(builtins.name, builtinsJar)
