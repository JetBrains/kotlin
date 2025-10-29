// ISSUE: KT-78127
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_PHASE: 2.2.0
// ^^^ KT-78127 is fixed in 2.3.0-Beta2
fun <T : Any> materialize(): T {
    return "OK" as T
}

var b = true

fun foo(x: String): String = x
fun foo(x: Int): Int = x

fun box(): String {
    var x: String? = null

    x = if (b) materialize() else throw Exception()

    return foo(x)
}
