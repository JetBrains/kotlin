fun foo(first: Int, second: Int, third: String) {
}

fun test(p: Int) = foo(<caret>second = 3)

// EXIST: p
// EXIST: first
// EXIST: second
// EXIST: third
