import java.io.File
import java.util.jar.JarFile

fun main(args: Array<String>) {
    val jar = File(args[0])
    // if "includeRuntime" is specified to be true
    var shouldIncludeRuntime = args[1].toBoolean()
    val hasKotlinPackage = JarFile(jar).entries().toList().any { it.name.startsWith("kotlin/") }
    if (shouldIncludeRuntime != hasKotlinPackage) {
        println(
            "Error: Kotlin runtime is expected to be included only if the attribute \"includeRuntime\" is specified to be true\n" +
                    "includeRuntime = ${shouldIncludeRuntime}; hasKotlinPackage = $hasKotlinPackage"
        )
    }
}
