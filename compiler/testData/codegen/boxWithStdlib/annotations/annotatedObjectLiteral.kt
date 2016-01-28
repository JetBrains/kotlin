annotation class Ann(val v: String = "???")
@Ann open class My
fun box(): String {
    val v = @Ann("OK") object: My() {}
    val klass = v.javaClass

    val annotations = klass.annotations
    // Ann, kotlin.Metadata, kotlin.jvm.internal.KotlinClass
    if (annotations.size != 3) return "Fail annotations size is ${annotations.size}: ${annotations.toList()}"
    val annotation = annotations.filterIsInstance<Ann>().firstOrNull()
                     ?: return "Fail no @Ann: ${annotations.toList()}"

    return annotation.v
}

