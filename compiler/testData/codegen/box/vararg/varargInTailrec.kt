// ISSUE: KT-63973
// WITH_STDLIB

tailrec fun <T> foo(vararg items: String, fn: (String) -> T): T = when (items.size) {
    0 -> fn("")
    else -> foo(*items.drop(1).toTypedArray()) {
        fn(items.first())
    }
}

fun box(): String {
    val result = buildString {
        foo("abcde") { append(it) }
    }
    if (result != "abcde") {
        return "Fail: $result"
    }
    return "OK"
}
