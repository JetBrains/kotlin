fun small(first: Int, second: Int) {
}

fun test() = small(12, <caret>)

// ABSENT: first
// EXIST: second