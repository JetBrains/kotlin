// IS_APPLICABLE: false
fun test(v: Boolean): (String) -> Int {
    return when (v) {
        true -> { { x -> taskOne(x) } }
        false -> { <caret>x -> taskTwo(x) }
    }
}

fun taskOne(s: String) = s.length
fun taskTwo(s: String) = 42
