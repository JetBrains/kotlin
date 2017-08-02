
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.serialization.builtins.BuiltInsSerializer
import org.gradle.jvm.tasks.Jar
import java.io.File

apply { plugin("kotlin") }

val builtinsSrc = File(rootDir, "core", "builtins", "src")
val builtinsNative = File(rootDir, "core", "builtins", "native")
// TODO: rewrite dependent projects on using build results instead of the fixed location
val builtinsSerialized = File(rootProject.extra["distDir"].toString(), "builtins")

val builtins by configurations.creating

dependencies {
    val compile by configurations
    compile(protobufLite())
    compile(files(builtinsSerialized))
}

configureKotlinProjectSources("core/builtins/src", "core/runtime.jvm/src", sourcesBaseDir = rootDir)
configureKotlinProjectResources(listOf(builtinsSerialized))
configureKotlinProjectNoTests()

val serialize = task("serialize") {
    val outDir = builtinsSerialized
    val inDirs = arrayOf(builtinsSrc, builtinsNative)
    outputs.file(outDir)
    inputs.files(*inDirs)
    doLast {
        System.setProperty("kotlin.colors.enabled", "false")
        BuiltInsSerializer(dependOnOldBuiltIns = false)
                .serialize(outDir, inDirs.asList(), listOf()) { totalSize, totalFiles ->
                    println("Total bytes written: $totalSize to $totalFiles files")
                }
    }
}

tasks.withType<JavaCompile> {
    dependsOn(protobufLiteTask)
    dependsOn(serialize)
}

tasks.withType<KotlinCompile> {
    dependsOn(protobufLiteTask)
    dependsOn(serialize)
}

val jar: Jar by tasks
jar.apply {
    dependsOn(serialize)
    from(builtinsSerialized) { include("kotlin/**") }
}

val builtinsJar by task<Jar> {
    dependsOn(serialize)
    from(builtinsSerialized) { include("kotlin/**") }
    baseName = "platform-builtins"
    destinationDir = File(buildDir, "libs")
}

artifacts.add(builtins.name, builtinsJar)
