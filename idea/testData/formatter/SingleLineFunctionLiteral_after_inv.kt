fun test(some: (Int) -> Int) {
}

fun foo() = test() {it}
val function = test {(a: Int) -> a}
val function1 = test {a : Int -> a}

// SET_TRUE: INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD