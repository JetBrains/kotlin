// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    val x: Array<List<*>> = arrayOf(listOf(1))
    val y : Array<in List<String>> = x

    if (y.size != 1) return "fail 1"

    y[0] = listOf("OK")

    return x[0][0] as String
}
