// IS_APPLICABLE: false
fun test(v: Boolean): (String) -> Int {
    return when (v) {
        true -> <caret>{ { taskOne(it) } }
        false -> { x -> taskTwo(x) }
    }
}

fun taskOne(s: String) = s.length
fun taskTwo(s: String) = 42