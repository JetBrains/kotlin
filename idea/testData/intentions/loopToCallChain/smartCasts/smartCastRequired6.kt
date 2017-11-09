// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<Any>) {
    var v: String?

    v = null
    <caret>for (o in list) {
        if (bar(o as String)) {
            v = o
            break
        }
    }
}

fun bar(s: String): Boolean = true