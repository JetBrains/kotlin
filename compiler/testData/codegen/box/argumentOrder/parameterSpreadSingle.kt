var calls = 0

data class Args(val a: String, val b: String)

fun buildArgs(): Args {
    calls += 1
    return Args("A", "B")
}

fun foo(a: String, b: String): String {
    return a + b
}

fun box(): String {
    val result = foo(...buildArgs())
    return if (result == "AB" && calls == 1) "OK" else "fail: $result|$calls"
}
