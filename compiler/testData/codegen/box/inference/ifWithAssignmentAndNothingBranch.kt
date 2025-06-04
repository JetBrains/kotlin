// IGNORE_BACKEND_K2: ANY
// ISSUE: KT-78127

fun <T : Any> materialize(): T {
    return "OK" as T
}

var b = true

var s: Nothing? = null

fun box(): String {
    var x: String? = null

    x = if (b) materialize() else throw Exception()

    return x
}
