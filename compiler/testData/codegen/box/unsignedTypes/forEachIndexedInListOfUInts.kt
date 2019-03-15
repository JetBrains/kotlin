// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

fun box(): String {
    val a = listOf(1u, 2u, 3u)
    var sum = 0
    a.forEachIndexed { index, uInt ->
        sum = sum * 10 + (index + 1) * uInt.toInt()
    }
    if (sum != 149) throw AssertionError()
    return "OK"
}
