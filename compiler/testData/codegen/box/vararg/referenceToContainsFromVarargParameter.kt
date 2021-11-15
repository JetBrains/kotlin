// WITH_STDLIB

fun foo(l: List<String>, vararg values: Any): Boolean =
    l.any(values::contains)

fun box(): String {
    if (!foo(listOf("OK"), "OK")) return "fail 1"
    if (foo(listOf("none", "OK"))) return "fail 2"

    return "OK"
}