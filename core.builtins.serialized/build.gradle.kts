
import java.io.File
import java.lang.IllegalStateException

buildscript {
    repositories {
        mavenLocal()
        maven { setUrl(rootProject.extra["repo"]) }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-compiler:${rootProject.extra["kotlinVersion"]}")
    }
}

val serializedCfg = configurations.create("default")

val builtinsSrc = File(rootDir, "core/builtins/src")
val builtinsNative = File(rootDir, "core/builtins/native")

task("serialize-builtins") {
    val outDir = File(buildDir, "builtins")
    val inDirs = arrayOf(builtinsSrc, builtinsNative)
    outputs.file(outDir)
    inputs.files(*inDirs)
    doLast {
        org.jetbrains.kotlin.serialization.builtins.BuiltInsSerializer(dependOnOldBuiltIns = false)
                .serialize(outDir, inDirs.asList(), listOf()) { totalSize, totalFiles ->
                    println("Total bytes written: $totalSize to $totalFiles files")
                }
    }
}

defaultTasks("serialize-builtins")
