// ISSUE: KT-78127

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
