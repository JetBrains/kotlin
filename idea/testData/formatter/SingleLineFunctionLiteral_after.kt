fun test(some: (Int) -> Int) {
}

fun foo() = test() { it }

// SET_TRUE: INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD