import java.io.File
import java.util.jar.JarFile

fun main(args: Array<String>) {
    val jar = File(args[0])
    // if "includeRuntime" is specified to be true
    var specifiedBeTrue = args[1].toBoolean()
    for (entry in JarFile(jar).entries()) {
        if (!specifiedBeTrue && entry.name.startsWith("kotlin/")) {
            println("Error: Kotlin runtime is expected to be excluded if the attribute \"includeRuntime\" is not specified to be true")
            break;
        }
    }
}
