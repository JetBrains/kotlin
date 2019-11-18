// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

annotation class Ann(val v: String = "???")
@Ann open class My
fun box(): String {
    val v = @Ann("OK") object: My() {}
    val klass = v.javaClass

    val annotations = klass.annotations.toList()
    // Ann, kotlin.Metadata
    if (annotations.size != 2) return "Fail annotations size is ${annotations.size}: $annotations"
    val annotation = annotations.filterIsInstance<Ann>().firstOrNull()
                     ?: return "Fail no @Ann: $annotations"

    return annotation.v
}
