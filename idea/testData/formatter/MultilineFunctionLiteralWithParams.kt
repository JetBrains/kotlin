fun test(some: (Int) -> Int) {
}

fun foo() = test() {a -> if (true) { a } else { 1 }}

// SET_TRUE: INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD