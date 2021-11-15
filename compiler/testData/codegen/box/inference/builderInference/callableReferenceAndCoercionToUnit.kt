// !DIAGNOSTICS: -OPT_IN_USAGE_ERROR -UNUSED_EXPRESSION
// WITH_STDLIB

@OptIn(ExperimentalStdlibApi::class)
fun test(s: String?): Int {
    val list = buildList {
        s?.let(::add)
    }
    return list.size
}

fun box(): String {
    return when (test("hello")) {
        1 -> "OK"
        else -> "Error"
    }
}
