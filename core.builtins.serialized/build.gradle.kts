
import org.gradle.jvm.tasks.Jar
import java.io.File
import org.jetbrains.kotlin.serialization.builtins.BuiltInsSerializer

buildscript {
    repositories {
        mavenLocal()
        maven { setUrl(rootProject.extra["repo"]) }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-compiler:${rootProject.extra["kotlinVersion"]}")
    }
}

val mainCfg = configurations.create("default")

val builtinsSrc = File(rootDir, "core/builtins/src")
val builtinsNative = File(rootDir, "core/builtins/native")
val builtinsSerialized = File(buildDir, "builtins")
val builtinsJar = File(buildDir, "builtins.jar")

artifacts.add(mainCfg.name, builtinsJar)

val serialize = task("internal.serialize") {
    val outDir = builtinsSerialized
    val inDirs = arrayOf(builtinsSrc, builtinsNative)
    outputs.file(outDir)
    inputs.files(*inDirs)
    doLast {
        BuiltInsSerializer(dependOnOldBuiltIns = false)
                .serialize(outDir, inDirs.asList(), listOf()) { totalSize, totalFiles ->
                    println("Total bytes written: $totalSize to $totalFiles files")
                }
    }
}

val mainTask = task<Jar>("prepare") {
    dependsOn(serialize)
    from(builtinsSerialized)
    into(builtinsJar)
}

defaultTasks(mainTask.name)
