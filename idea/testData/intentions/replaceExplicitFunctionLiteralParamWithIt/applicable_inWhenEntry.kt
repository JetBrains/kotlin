fun test(v: Boolean): (String) -> Int {
    return when (v) {
        true -> { { <caret>x -> taskOne(x) } }
        false -> { x -> taskTwo(x) }
    }
}

fun taskOne(s: String) = s.length
fun taskTwo(s: String) = 42
