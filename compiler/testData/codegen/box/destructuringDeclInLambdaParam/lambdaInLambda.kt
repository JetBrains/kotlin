// ISSUE: KT-67185
// WITH_STDLIB

fun noop() {}

fun foo(list: List<Pair<String, String>>) {
    list.map { (a, b) -> { noop() } }
        .forEach { it() }
}

fun box(): String {
    val list = listOf("a" to "b", "c" to "")
    foo(list)
    return "OK"
}
