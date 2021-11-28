// IGNORE_BACKEND: JS

fun box(): String {
    var x = "c"
    val call = test(c = x, b = { x = "a"; "b" }(), a = x)
    return if (call == "abc") "OK" else "fail: $call != abc"
}

fun test(a: String, b: String, c: String): String =
    a + b + c
