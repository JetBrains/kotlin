// !LANGUAGE: +NewInference
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun test(foo: MutableList<String>?): List<String> {
    val bar = foo ?: listOf()
    return bar
}

fun box(): String {
    val a = test(null)
    if (a.isNotEmpty()) return "Fail 1"

    val b = test(mutableListOf("a"))
    if (b.size != 1) return "Fail 2"

    return "OK"
}