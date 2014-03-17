fun small(first: Int, second: Int) {
}

fun test() = small(12, <caret>)

// ABSENT: {"lookupString":"first = ","itemText":"first = "}
// EXIST: {"lookupString":"second = ","itemText":"second = "}